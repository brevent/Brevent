package me.piebridge.brevent.ui;

import android.accounts.NetworkErrorException;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Base64;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import me.piebridge.SimpleAdb;
import me.piebridge.SimpleSu;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventProtocol;

public class BreventIntentService extends IntentService {

    public static final int ID = 59526;

    public static final int ID2 = 59527;

    public static final int ID3 = 59528;

    private static final String CHANNEL_ID = "root";

    private static final int TIMEOUT = 42;

    private static final int ADB_TIMEOUT = 10;

    private static final int CHECK_TIMEOUT_MS = 42_000;

    private static final Object LOCK_BREVENT = new Object();

    private static final String ADB_DIRECTORY = "misc/adb";

    private static final String ADB_KEYS_FILE = "adb_keys";

    private ExecutorService executor = new ScheduledThreadPoolExecutor(0x1);

    private Future<?> future;

    public BreventIntentService() {
        super("BreventIntentService");
    }

    private boolean isStarted() {
        BreventApplication application = (BreventApplication) getApplication();
        return application.isStarted() || application.checkPortNE();
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        BreventApplication application = (BreventApplication) getApplication();
        hideStopped(application);
        String action = intent.getAction();
        Notification notification = postNotification(application);
        UILog.d("show notification");
        startForeground(ID, notification);
        if (SimpleSu.hasSu() && !application.checkPortNE()) {
            application.setStarted(false);
            synchronized (LOCK_BREVENT) {
                if (!isStarted()) {
                    try {
                        application.setStarting(true);
                        startBrevent(action);
                    } finally {
                        application.setStarting(false);
                    }
                }
            }
        }
        if (!SimpleSu.hasSu() || !isStarted()) {
            showStopped(application);
        }
        UILog.d("hide notification");
        stopForeground(true);
    }

    private void startBrevent(String action) {
        BreventApplication application = (BreventApplication) getApplication();
        if (BreventIntent.ACTION_RUN_AS_ROOT.equalsIgnoreCase(action)) {
            UILog.d("startBreventSync, action: " + action);
            application.notifyRootCompleted(startBreventSync(true));
        } else {
            UILog.d("startBrevent, action: " + action);
            startBrevent();
        }
    }

    static void sleep(int s) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(s));
        } catch (InterruptedException e) { // NOSONAR
            // do nothing
        }
    }

    private List<String> startBrevent() {
        if (future != null) {
            future.cancel(true);
        }
        final List<String> results = new ArrayList<>();
        future = executor.submit(new Runnable() {
            @Override
            public void run() {
                results.addAll(startBreventSync(false));
            }
        });
        long timeout = System.currentTimeMillis() + CHECK_TIMEOUT_MS;
        do {
            sleep(1);
            if (!results.isEmpty()) {
                return results;
            }
        } while (System.currentTimeMillis() < timeout);
        try {
            future.get(1, TimeUnit.SECONDS);
            return results;
        } catch (InterruptedException | ExecutionException e) {
            String msg = "(Can't start Brevent)";
            UILog.d(msg, e);
            return Collections.singletonList(msg);
        } catch (TimeoutException e) {
            String msg = "(Can't start Brevent in " + TIMEOUT + " seconds)";
            UILog.d(msg, e);
            return Collections.singletonList(msg);
        } finally {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    List<String> startBreventSync(boolean interactive) {
        if (isStarted()) {
            return Collections.singletonList("(Started)");
        }
        BreventApplication application = (BreventApplication) getApplication();
        String path = application.copyBrevent();
        if (path == null) {
            return Collections.singletonList("(Can't make brevent)");
        } else if (BuildConfig.RELEASE && BuildConfig.ADB_K != null && application.isRootAdb()) {
            return startBreventAdb(path, interactive);
        } else {
            return Collections.singletonList(startBreventRoot(path, interactive));
        }
    }

    private List<String> startBreventAdb(String path, boolean interactive) {
        boolean needStop = false;
        int port = AdbPortUtils.getAdbPort();
        if (port <= 0) {
            needStop = !AppsDisabledFragment.isAdbRunning();
            String message = SimpleSu.su("setprop service.adb.tcp.port 5555; " +
                    "setprop service.adb.brevent.close 1; " +
                    "setprop ctl.restart adbd", interactive);
            port = AdbPortUtils.getAdbPort();
            if (port <= 0) {
                if (TextUtils.isEmpty(message)) {
                    ((BreventApplication) getApplication()).setRootAdb(false);
                    return Collections.singletonList(startBreventRoot(path, interactive));
                } else {
                    return Collections.singletonList(message);
                }
            }
        }
        if (interactive) {
            makeSureKeys();
        }
        String message = "(Can't adb)";
        BreventApplication application = (BreventApplication) getApplication();
        application.setAdb(needStop);
        String command = "sh " + path;
        SimpleAdb simpleAdb = new SimpleAdb(BuildConfig.ADB_K, BuildConfig.ADB_M, BuildConfig.ADB_D);
        boolean fail = true;
        for (int i = 0; i < ADB_TIMEOUT; ++i) {
            try {
                String adb = simpleAdb.exec(port, command);
                if (adb != null) {
                    message = adb;
                    for (String s : adb.split(System.lineSeparator())) {
                        UILog.d(s);
                    }
                    fail = adb.contains("pm path");
                    if (adb.contains("run as root")) {
                        ((BreventApplication) getApplication()).setRootAdb(false);
                    }
                }
                break;
            } catch (ConnectException e) {
                UILog.d("Can't adb(Connection refused)", e);
            } catch (IOException e) {
                UILog.w("Can't adb(" + e.getMessage() + ")", e);
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                message = sw.toString();
            }
            sleep(1);
        }
        if (isStarted()) {
            UILog.d("adb success");
            return Collections.singletonList(message);
        } else if (!fail) {
            application.setStarted(true);
            UILog.d("adb no fail");
            return Collections.singletonList(message);
        } else {
            UILog.d("adb fail, fallback to direct root");
            List<String> messages = new ArrayList<>();
            messages.add(message);
            messages.add(System.lineSeparator());
            messages.add(startBreventRoot(path, interactive));
            return messages;
        }
    }

    private boolean makeSureKeys() {
        File keyFile = getUserKeyFile();
        if (keyFile == null) {
            return false;
        }
        String keys = Base64.encodeToString(BuildConfig.ADB_K, Base64.NO_WRAP);
        String command = "file=" + keyFile.getAbsolutePath() + "; " +
                "keys=" + keys + "; " +
                "if [ ! -f $file ]; then " +
                "echo $keys >> $file; chown 1000:2000 $file; chmod 0640 $file; " +
                "else " +
                "grep -q $keys $file || echo $keys >> $file; " +
                "fi";
        SimpleSu.su(command);
        return true;
    }

    private String startBreventRoot(String path, boolean interactive) {
        return SimpleSu.su("setprop service.adb.brevent.close -1; "
                + "$SHELL " + path + " || sh " + path, interactive);
    }

    static NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static Notification.Builder buildNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getNotificationManager(context);
            NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
            if (channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_LOW) {
                nm.deleteNotificationChannel(CHANNEL_ID);
            }
            channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.brevent),
                    NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
            return new Notification.Builder(context, CHANNEL_ID);
        } else {
            return buildNotificationDeprecation(context);
        }
    }

    @SuppressWarnings("deprecation")
    private static Notification.Builder buildNotificationDeprecation(Context context) {
        Notification.Builder builder = new Notification.Builder(context);
        builder.setPriority(Notification.PRIORITY_MAX);
        return builder;
    }

    private static Notification postNotification(Context context) {
        Notification.Builder builder = buildNotification(context);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setSmallIcon(BuildConfig.IC_STAT);
        builder.setContentTitle(context.getString(R.string.brevent_status_starting));
        return builder.build();
    }

    private static void showStopped(Context context) {
        Notification.Builder builder = buildNotification(context);
        builder.setAutoCancel(true);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setSmallIcon(BuildConfig.IC_STAT);
        builder.setContentTitle(context.getString(R.string.brevent_status_not_started));
        builder.setContentIntent(PendingIntent.getActivity(context, 0,
                new Intent(context, BreventActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        Notification notification = builder.build();
        getNotificationManager(context).notify(ID3, notification);
    }

    private static void hideStopped(Context context) {
        getNotificationManager(context).cancel(ID3);
    }

    public static void startBrevent(BreventApplication application, String action) {
        Intent intent = new Intent(application, BreventIntentService.class);
        intent.setAction(action);
        if (shouldForeground()) {
            UILog.d("will startForegroundService");
            application.startForegroundService(intent);
        } else {
            UILog.d("will startService");
            application.startService(intent);
        }
    }

    private static boolean shouldForeground() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static void checkStopped(BreventApplication application) {
        try {
            sleep(0x1);
            if (!application.isStarting() && !application.checkPort(true)) {
                showStopped(application);
                BreventActivity.cancelAlarm(application);
            }
        } catch (NetworkErrorException e) {
            UILog.w("brevent checking timeout");
        }
    }

    public static boolean checkBrevent(BreventApplication application) {
        try {
            for (int i = 0; i < 0x5; ++i) {
                if (application.checkPort(true)) {
                    UILog.d("brevent worked");
                    return true;
                }
                sleep(0x1);
            }
            showNoBrevent(application, true);
        } catch (NetworkErrorException e) {
            UILog.w("brevent checking timeout");
        }
        return false;
    }

    private File getUserKeyFile() {
        File adbDir = new File(Environment.getDataDirectory(), ADB_DIRECTORY);
        if (adbDir.exists()) {
            return new File(adbDir, ADB_KEYS_FILE);
        } else {
            return null;
        }
    }

    private static void showNoBrevent(Context context, boolean exit) {
        UILog.d("no brevent, exit: " + exit);
        Notification.Builder builder = buildNotification(context);
        builder.setAutoCancel(true);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setSmallIcon(BuildConfig.IC_STAT);
        int title = exit ? R.string.brevent_status_stopped : R.string.brevent_status_unknown;
        builder.setContentTitle(context.getString(title));
        File file = AppsActivityHandler.fetchLogs(context);
        if (BuildConfig.RELEASE && file != null) {
            builder.setContentText(context.getString(R.string.brevent_status_report));
            Intent intent = new Intent(context, BreventActivity.class);
            intent.setAction(BreventIntent.ACTION_FEEDBACK);
            intent.putExtra(BreventIntent.EXTRA_PATH, file.getPath());
            builder.setContentIntent(PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT));
        }
        Notification notification = builder.build();
        getNotificationManager(context).notify(ID2, notification);
        if (exit) {
            BreventActivity.cancelAlarm(context);
        }
    }

}
