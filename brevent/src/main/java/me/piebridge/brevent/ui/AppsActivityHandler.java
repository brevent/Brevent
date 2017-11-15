package me.piebridge.brevent.ui;

import android.Manifest;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.WorkerThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import me.piebridge.SimpleAdb;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.protocol.BreventProtocol;
import me.piebridge.brevent.protocol.BreventRequest;

/**
 * Created by thom on 2017/2/3.
 */
public class AppsActivityHandler extends Handler {

    private static final String[][] LOGS = new String[][] {
            {"system.txt", "-b system"},
            {"events.txt", "-b events am_pss:s"},
            {"crash.txt", "-b crash"},
            {"brevent.txt", "-b main -s BreventServer BreventLoader BreventUI"}
    };

    private static final String[][] DUMPS = new String[][] {
            {"battery.txt", "batterystats", null},
    };

    private static final long DELAY = 15000;

    private static final int TIMEOUT = 5000;

    private static final int RETRY = 1000;

    private static final int SHORT = 250;

    private final Handler uiHandler;

    private final WeakReference<BreventActivity> mReference;

    private boolean hasResponse;

    private String adb;

    private boolean adbing;

    private boolean adbChecked;

    private Thread adbThread;

    public AppsActivityHandler(BreventActivity activity, Handler handler) {
        super(newLooper());
        mReference = new WeakReference<>(activity);
        uiHandler = handler;
    }

    private static Looper newLooper() {
        HandlerThread thread = new HandlerThread("Brevent");
        thread.start();
        return thread.getLooper();
    }

    @Override
    public void handleMessage(Message message) {
        BreventActivity activity = mReference.get();
        switch (message.what) {
            case BreventActivity.MESSAGE_RETRIEVE:
                if (BuildConfig.RELEASE && !adbChecked && activity != null) {
                    Boolean checked = checkPort(activity);
                    if (checked == null) {
                        break;
                    } else if (!checked) {
                        checkAdb(activity);
                    }
                }
                removeMessages(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE);
                UILog.d("request status");
                if (activity != null) {
                    requestStatus(false, activity.isConfirmed(), activity.isCheck());
                }
                break;
            case BreventActivity.MESSAGE_RETRIEVE2:
                removeMessages(BreventActivity.MESSAGE_RETRIEVE2);
                UILog.d("retry request status");
                hasResponse = false;
                if (activity != null) {
                    requestStatus(true, activity.isConfirmed(), activity.isCheck());
                }
                break;
            case BreventActivity.MESSAGE_BREVENT_RESPONSE:
                if (!hasResponse) {
                    UILog.d("received response");
                    hasResponse = true;
                    ((BreventApplication) activity.getApplication()).onStarted();
                }
                removeMessages(BreventActivity.MESSAGE_RETRIEVE2);
                removeMessages(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE);
                BreventProtocol response = (BreventProtocol) message.obj;
                if (activity != null && !activity.isStopped()) {
                    activity.onBreventResponse(response);
                }
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_HIDE_DISABLED);
                break;
            case BreventActivity.MESSAGE_BREVENT_NO_RESPONSE:
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_NO_BREVENT_DATA);
                break;
            case BreventActivity.MESSAGE_BREVENT_REQUEST:
                send((BreventProtocol) message.obj);
                break;
            case BreventActivity.MESSAGE_ROOT_COMPLETED:
                if (!hasResponse) {
                    uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_ROOT_COMPLETED, message.obj)
                            .sendToTarget();
                }
                break;
            case BreventActivity.MESSAGE_LOGS:
                File path = fetchLogs(activity);
                uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_LOGS, path).sendToTarget();
                break;
            case BreventActivity.MESSAGE_REMOVE_ADB:
                adbing = false;
                adbThread.interrupt();
                break;
            default:
                break;
        }
    }

    public static File fetchLogs(Context context) {
        File path = null;
        File dir;
        if (context != null && (dir = context.getExternalFilesDir("logs")) != null) {
            DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmm", Locale.US);
            String date = df.format(Calendar.getInstance().getTime());
            File cacheDir = context.getCacheDir();
            try {
                for (String[] log : LOGS) {
                    File file = new File(cacheDir, date + "." + log[0]);
                    String command = "/system/bin/logcat -d -v threadtime -f "
                            + file.getPath() + " " + log[1];
                    UILog.d("logcat for " + log[0]);
                    Runtime.getRuntime().exec(command).waitFor();
                }
                if (context.getPackageManager().checkPermission(Manifest.permission.DUMP,
                        BuildConfig.APPLICATION_ID) == PackageManager.PERMISSION_GRANTED) {
                    for (String[] dump : DUMPS) {
                        File file = new File(cacheDir, date + "." + dump[0]);
                        String[] args = dump[2] == null ? null : dump[2].split(" ");
                        BreventApplication.dumpsys(dump[1], args, file);
                    }
                } else {
                    UILog.d("skip for dump");
                }
                Runtime.getRuntime().exec("sync").waitFor();
                path = zipLog(context, dir, date);
            } catch (IOException | InterruptedException e) {
                UILog.w("Can't get logs", e);
            }
        }
        return path;
    }

    private boolean checkAdb(BreventActivity activity) {
        final int port = AdbPortUtils.getAdbPort();
        final String path = ((BreventApplication) activity.getApplication()).copyBrevent();
        if (port > 0 && path != null) {
            adbing = true;
            uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_SHOW_PROGRESS_ADB);
            adbThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    doAdb(port, path);
                }
            });
            adbThread.start();
            sendEmptyMessageDelayed(BreventActivity.MESSAGE_REMOVE_ADB, DELAY);
            adbChecked = true;
            return true;

        }
        return false;
    }

    void doAdb(int port, String path) {
        SimpleAdb simpleAdb = new SimpleAdb(BuildConfig.ADB_K, BuildConfig.ADB_M, BuildConfig.ADB_D);
        try {
            adb = simpleAdb.exec(port, "sh " + path);
        } catch (IOException e) {
            UILog.e("Can't adb", e);
        } finally {
            adbing = false;
            if (!hasResponse) {
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_SHOW_PROGRESS);
            }
        }
    }

    private static File zipLog(Context context, File dir, String date) {
        String[] names = dir.list();
        if (names != null) {
            for (String name : names) {
                File file = new File(dir, name);
                if (name.startsWith("logs-v") && file.isFile() && file.delete()) {
                    UILog.d("delete file " + file.getName());
                }
            }
        }
        try {
            File path = new File(dir, "logs-v" + BuildConfig.VERSION_NAME + "-" + date + ".zip");
            try (
                    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path))
            ) {
                for (String[] log : LOGS) {
                    zipLog(context, zos, date, log[0]);
                }
                for (String[] dump : DUMPS) {
                    zipLog(context, zos, date, dump[0]);
                }
                File parent = context.getExternalFilesDir(null).getParentFile();
                File[] files = parent.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String name = file.getName();
                        if (name.startsWith("server.") && name.endsWith(".txt")) {
                            zipLog(zos, file);
                        }
                    }
                }
            }
            return path;
        } catch (IOException e) {
            UILog.e("Can't report bug", e);
            return null;
        }
    }

    private static void zipLog(Context context, ZipOutputStream zos, String date, String path)
            throws IOException {
        File file = new File(context.getCacheDir(), date + "." + path);
        if (!file.exists()) {
            UILog.w("Can't find " + file.getPath());
            return;
        }
        zipLog(zos, file);
        file.delete();
    }

    private static void zipLog(ZipOutputStream zos, File file) throws IOException {
        zos.putNextEntry(new ZipEntry(file.getName()));
        try (
                InputStream is = new FileInputStream(file)
        ) {
            int length;
            byte[] buffer = new byte[0x1000];
            while ((length = is.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry();
    }

    private void requestStatus(boolean retry, boolean confirmed, boolean check) {
        BreventRequest request = new BreventRequest(confirmed, check);
        request.retry = retry;
        send(request);
    }

    private Boolean checkPort(BreventActivity activity) {
        uiHandler.sendEmptyMessageDelayed(BreventActivity.UI_MESSAGE_CHECKING_BREVENT, SHORT);
        try {
            return ((BreventApplication) (activity.getApplication())).checkPort();
        } catch (NetworkErrorException e) {
            UILog.d("Can't check port: " + e.getMessage(), e);
            uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_NO_LOCAL_NETWORK);
            return null;
        } finally {
            uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_CHECKED_BREVENT);
        }
    }

    @WorkerThread
    private boolean send(BreventProtocol message) {
        BreventActivity activity = mReference.get();
        if (activity == null || activity.isStopped()) {
            UILog.w("Can't send request now");
            return false;
        }
        boolean timeout = false;
        int action = message.getAction();
        Boolean checked = checkPort(activity);
        if (checked == null) {
            return false;
        } else if (!checked) {
            onConnectError(activity);
            onFinal(action, false);
            return false;
        }
        try (
                Socket socket = new Socket(InetAddress.getLoopbackAddress(), BreventProtocol.PORT);
        ) {
            if (action == BreventProtocol.STATUS_REQUEST) {
                socket.setSoTimeout(TIMEOUT);
            }
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            BreventProtocol.writeTo(message, os);
            os.flush();
            if (action != BreventProtocol.STATUS_REQUEST) {
                sendEmptyMessageDelayed(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE, DELAY);
            } else {
                sendEmptyMessageDelayed(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE, DELAY * 2);
            }

            DataInputStream is = new DataInputStream(socket.getInputStream());
            BreventProtocol response = BreventProtocol.readFrom(is);
            os.close();
            is.close();
            if (response == null && !message.retry) {
                message.retry = true;
                send(message);
            } else {
                obtainMessage(BreventActivity.MESSAGE_BREVENT_RESPONSE, response).sendToTarget();
            }
            return true;
        } catch (ConnectException e) {
            // shouldn't happen
            UILog.v("Can't connect to localhost:" + BreventProtocol.PORT, e);
            onConnectError(activity);
        } catch (SocketTimeoutException e) {
            timeout = true;
            hasResponse = false;
            UILog.v("timeout to localhost:" + BreventProtocol.PORT, e);
        } catch (IOException e) {
            hasResponse = false;
            UILog.v("io error to localhost:" + BreventProtocol.PORT, e);
            uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_IO_BREVENT, e).sendToTarget();
        }
        onFinal(action, timeout);
        return false;
    }

    private void onConnectError(BreventActivity activity) {
        hasResponse = false;
        if (!adbing) {
            if (adb != null) {
                uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_SHELL_COMPLETED, adb)
                        .sendToTarget();
                adb = null;
            } else if (!((BreventApplication) activity.getApplication()).isRunningAsRoot()) {
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_NO_BREVENT);
            }
        }
    }

    private void onFinal(int action, boolean timeout) {
        if (action == BreventProtocol.STATUS_REQUEST) {
            sendEmptyMessageDelayed(BreventActivity.MESSAGE_RETRIEVE2,
                    (!timeout && AppsDisabledFragment.isEmulator()) ? TIMEOUT : RETRY);
        }
    }

}
