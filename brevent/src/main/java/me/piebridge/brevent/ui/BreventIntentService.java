package me.piebridge.brevent.ui;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
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
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import me.piebridge.LogReader;
import me.piebridge.SimpleAdb;
import me.piebridge.SimpleSu;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventIntent;

public class BreventIntentService extends IntentService {

    public static final int ID = 59526;

    public static final int ID2 = 59527;

    public static final int ID3 = 59528;

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
        SimpleSu.setKill(new Runnable() {
            @Override
            public void run() {
                LogReader.killDescendants(android.os.Process.myPid());
            }
        });
    }

    private boolean isStarted() {
        BreventApplication application = (BreventApplication) getApplication();
        return application.isStarted() || application.checkPort();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        BreventApplication application = (BreventApplication) getApplication();
        hideStopped(application);
        String action = intent.getAction();
        Notification notification = postNotification(application);
        UILog.i("show notification");
        startForeground(ID, notification);
        if (SimpleSu.hasSu() && !application.checkPort()) {
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
        UILog.i("hide notification");
        stopForeground(true);
    }

    private void startBrevent(String action) {
        BreventApplication application = (BreventApplication) getApplication();
        if (BreventIntent.ACTION_RUN_AS_ROOT.equalsIgnoreCase(action)) {
            UILog.i("startBreventSync, action: " + action);
            application.notifyRootCompleted(startBreventSync());
        } else {
            UILog.i("startBrevent, action: " + action);
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
                results.addAll(startBreventSync());
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
            UILog.i(msg, e);
            return Collections.singletonList(msg);
        } catch (TimeoutException e) {
            String msg = "(Can't start Brevent in " + TIMEOUT + " seconds)";
            UILog.i(msg, e);
            return Collections.singletonList(msg);
        } finally {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    List<String> startBreventSync() {
        if (isStarted()) {
            return Collections.singletonList("(Started)");
        }
        BreventApplication application = (BreventApplication) getApplication();
        String path = application.copyBrevent();
        if (path == null) {
            return Collections.singletonList("(Can't make brevent)");
        } else if (BuildConfig.RELEASE && BuildConfig.ADB_K != null) {
            return startBreventAdb(path);
        } else {
            return Collections.singletonList(startBreventRoot(path));
        }
    }

    private List<String> startBreventAdb(String path) {
        boolean needStop = false;
        int port = AdbPortUtils.getAdbPort();
        if (port <= 0) {
            needStop = !AppsDisabledFragment.isAdbRunning();
            String message = SimpleSu.su("setprop service.adb.tcp.port 5555; " +
                    "setprop service.adb.brevent.close 1; " +
                    "setprop ctl.restart adbd" + makeSureKeys(), true);
            port = AdbPortUtils.getAdbPort();
            if (port <= 0) {
                if (TextUtils.isEmpty(message)) {
                    return Collections.singletonList(startBreventRoot(path));
                } else {
                    return Collections.singletonList(message);
                }
            }
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
                        UILog.i(s);
                    }
                    fail = adb.contains("pm path");
                }
                break;
            } catch (ConnectException e) {
                UILog.w(formatAdbException(port, e));
            } catch (IOException e) {
                UILog.w(formatAdbException(port, e), e);
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                message = sw.toString();
            }
            sleep(1);
        }
        if (isStarted()) {
            UILog.i("adb success");
            return Collections.singletonList(message);
        } else if (!fail) {
            application.setStarted(true);
            UILog.i("adb no fail");
            return Collections.singletonList(message);
        } else {
            UILog.i("adb fail, fallback to direct root");
            application.stopAdbIfNeeded();
            List<String> messages = new ArrayList<>();
            messages.add(message);
            messages.add(System.lineSeparator());
            messages.add(startBreventRoot(path));
            return messages;
        }
    }

    static String formatAdbException(int port, Exception e) {
        return "Can't adb(" + e.getMessage() + ") to localhost:" + port;
    }

    private String makeSureKeys() {
        File keyFile = getUserKeyFile();
        if (keyFile == null) {
            return "";
        }
        String keys = Base64.encodeToString(BuildConfig.ADB_K, Base64.NO_WRAP);
        return "\nfile=" + keyFile.getAbsolutePath() + "; " +
                "keys=" + keys + "; " +
                "if [ ! -f $file ]; then " +
                "echo $keys >> $file; chown 1000:2000 $file; chmod 0640 $file; " +
                "else " +
                "grep -q $keys $file || echo $keys >> $file; " +
                "fi";
    }

    private String startBreventRoot(String path) {
        return SimpleSu.su("setprop service.adb.brevent.close -1; "
                + "$SHELL " + path + " || /system/bin/sh " + path, true);
    }

    static NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static Notification.Builder buildNotification(Context context) {
        return buildNotification(context, "root", R.string.notification_brevent_server,
                NotificationManager.IMPORTANCE_LOW);
    }

    static Notification.Builder buildNotification(Context context, String channelId, int resId,
                                                  int priority) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getNotificationManager(context);
            NotificationChannel channel = nm.getNotificationChannel(channelId);
            String name = context.getString(resId);
            if (channel == null || !Objects.equals(channel.getName(), name)) {
                if (channel != null) {
                    nm.deleteNotificationChannel(channelId);
                }
                channel = new NotificationChannel(channelId, name, priority);
                nm.createNotificationChannel(channel);
            }
            return new Notification.Builder(context, channelId);
        } else {
            return buildNotificationDeprecation(context, getPriorityDeprecated(priority));
        }
    }

    private static int getPriorityDeprecated(int priority) {
        switch (priority) {
            case NotificationManager.IMPORTANCE_MIN:
                return Notification.PRIORITY_MIN;
            case NotificationManager.IMPORTANCE_LOW:
                return Notification.PRIORITY_LOW;
            case NotificationManager.IMPORTANCE_HIGH:
                return Notification.PRIORITY_HIGH;
            case NotificationManager.IMPORTANCE_DEFAULT:
            default:
                return Notification.PRIORITY_DEFAULT;
        }
    }

    @SuppressWarnings("deprecation")
    private static Notification.Builder buildNotificationDeprecation(Context context, int priority) {
        return new Notification.Builder(context).setPriority(priority);
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
            UILog.i("will startForegroundService");
            application.startForegroundService(intent);
        } else {
            UILog.i("will startService");
            application.startService(intent);
        }
    }

    private static boolean shouldForeground() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static void checkStopped(BreventApplication application) {
        sleep(0x1);
        if (!application.isStarting() && !application.checkPort(true)) {
            showStopped(application);
            BreventActivity.cancelAlarm(application);
        }
    }

    public static boolean checkBrevent(BreventApplication application) {
        for (int i = 0; i < 0x5; ++i) {
            if (application.checkPort(true)) {
                UILog.i("brevent worked");
                return true;
            }
            sleep(0x1);
        }
        showNoBrevent(application, true);
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
        UILog.i("no brevent, exit: " + exit);
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
