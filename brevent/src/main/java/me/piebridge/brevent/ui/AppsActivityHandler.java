package me.piebridge.brevent.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

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

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.protocol.BreventProtocol;
import me.piebridge.brevent.protocol.BreventRequest;

/**
 * Created by thom on 2017/2/3.
 */
public class AppsActivityHandler extends Handler {

    private boolean checked;

    private static final String[][] LOGS = new String[][] {
            {"system.txt", "-b system"},
            {"events.txt", "-b events am_pss:s"},
            {"brevent.txt", "-b main -s BreventServer BreventLoader BreventUI"}
    };

    private static final long DELAY = 15000;

    private static final int TIMEOUT = 5000;

    private static final int RETRY = 1000;

    private final Handler uiHandler;

    private final WeakReference<BreventActivity> mReference;

    private boolean hasResponse;

    private String adb;

    private boolean adbing;

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
                if (!checked && !checkPort() && activity != null) {
                    ((BreventApplication) activity.getApplication()).copyBrevent();
                    checkAdb();
                }
                removeMessages(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE);
                UILog.d("request status");
                requestStatus(false);
                break;
            case BreventActivity.MESSAGE_RETRIEVE2:
                UILog.d("retry request status");
                hasResponse = false;
                requestStatus(true);
                break;
            case BreventActivity.MESSAGE_BREVENT_RESPONSE:
                if (!hasResponse) {
                    UILog.d("received response");
                    hasResponse = true;
                    hideNotification();
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
                File path = null;
                File dir;
                if (activity != null && (dir = activity.getExternalFilesDir("logs")) != null) {
                    DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmm", Locale.US);
                    String date = df.format(Calendar.getInstance().getTime());
                    File cacheDir = activity.getCacheDir();
                    try {
                        for (String[] log : LOGS) {
                            File file = new File(cacheDir, date + "." + log[0]);
                            String command = "/system/bin/logcat -d -v threadtime -f "
                                    + file.getPath() + " " + log[1];
                            UILog.d("logcat for " + log[0]);
                            Runtime.getRuntime().exec(command).waitFor();
                        }
                        Runtime.getRuntime().exec("sync").waitFor();
                        path = zipLog(activity, dir, date);
                    } catch (IOException | InterruptedException e) {
                        UILog.w("Can't get logs", e);
                    }
                }
                uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_LOGS, path).sendToTarget();
                break;
            default:
                break;
        }
    }

    private boolean checkAdb() {
        String port = SystemProperties.get("service.adb.tcp.port", "");
        UILog.d("service.adb.tcp.port: " + port);
        if (!TextUtils.isEmpty(port) && TextUtils.isDigitsOnly(port)) {
            final int p = Integer.parseInt(port);
            if (p > 0 && p <= 0xffff) {
                adbing = true;
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_SHOW_PROGRESS_ADB);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            adb = new SimpleAdb(p).run();
                        } catch (IOException e) {
                            UILog.d("Can't adb", e);
                        } finally {
                            adbing = false;
                            uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_SHOW_PROGRESS);
                        }
                    }
                }).start();
                checked = true;
                return true;
            }

        }
        return false;
    }

    private File zipLog(Context context, File dir, String date) {
        try {
            File path = new File(dir, "logs-v" + BuildConfig.VERSION_NAME + "-" + date + ".zip");
            try (
                    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path))
            ) {
                for (String[] log : LOGS) {
                    File file = new File(context.getCacheDir(), date + "." + log[0]);
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
                    file.delete();
                }
            }
            return path;
        } catch (IOException e) {
            UILog.e("cannot report bug", e);
            return null;
        }
    }

    private void requestStatus(boolean retry) {
        BreventRequest request = new BreventRequest();
        request.retry = retry;
        send(request);
    }

    private void hideNotification() {
        BreventActivity activity = mReference.get();
        if (activity != null) {
            ((NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE))
                    .cancel(BreventIntentService.ID);
        }
    }

    @WorkerThread
    public static boolean checkPort() {
        try (
                Socket socket = new Socket(InetAddress.getLoopbackAddress(), BreventProtocol.PORT);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                DataInputStream is = new DataInputStream(socket.getInputStream())
        ) {
            os.writeShort(0);
            os.flush();
            BreventProtocol.readFrom(is);
            UILog.d("connected to localhost: " + BreventProtocol.PORT);
            return true;
        } catch (ConnectException e) {
            UILog.v("cannot connect to localhost:" + BreventProtocol.PORT, e);
            return false;
        } catch (IOException e) {
            UILog.v("io error to localhost:" + BreventProtocol.PORT, e);
            return false;
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
            hasResponse = false;
            UILog.v("cannot connect to localhost:" + BreventProtocol.PORT, e);
            UILog.d("adbing: " + adbing);
            if (!adbing) {
                if (adb != null) {
                    uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_SHELL_COMPLETED, adb)
                            .sendToTarget();
                    adb = null;
                } else if (!message.retry) {
                    uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_NO_BREVENT);
                }
            }
        } catch (SocketTimeoutException e) {
            timeout = true;
            hasResponse = false;
            UILog.v("timeout to localhost:" + BreventProtocol.PORT, e);
        } catch (IOException e) {
            hasResponse = false;
            UILog.v("io error to localhost:" + BreventProtocol.PORT, e);
            uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_IO_BREVENT, e).sendToTarget();
        }
        if (action == BreventProtocol.STATUS_REQUEST) {
            sendEmptyMessageDelayed(BreventActivity.MESSAGE_RETRIEVE2,
                    (!timeout && AppsDisabledFragment.isEmulator()) ? TIMEOUT : RETRY);
        }
        return false;
    }

}
