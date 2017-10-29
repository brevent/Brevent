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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import eu.chainfire.libsuperuser.Shell;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventProtocol;

public class BreventIntentService extends IntentService {

    public static final int ID = 59526;

    public static final int ID2 = 59527;

    public static final int ID3 = 59528;

    private static final String CHANNEL_ID = "root";

    private static final int TIMEOUT = 15;

    private static final int ADB_TIMEOUT = 6;

    private static final int CHECK_TIMEOUT_MS = 15_000;

    private static final String FIXO = "pbd=`pidof brevent_daemon`; " +
            "pbs=`pidof brevent_server`; " +
            "pin=`pidof installd`; " +
            "echo $pbd > /acct/uid_0/pid_$pin/tasks; " +
            "echo $pbd > /acct/uid_0/pid_$pin/cgroup.procs; " +
            "echo $pbs > /acct/uid_0/pid_$pin/tasks; " +
            "echo $pbs > /acct/uid_0/pid_$pin/cgroup.procs";

    private static final String ADB_DIRECTORY = "misc/adb";

    private static final String ADB_KEYS_FILE = "adb_keys";

    private ExecutorService executor = new ScheduledThreadPoolExecutor(0x1);

    private Future<?> future;

    public BreventIntentService() {
        super("BreventIntentService");
    }

    private boolean checkPort() {
        try {
            return ((BreventApplication) getApplication()).checkPort();
        } catch (NetworkErrorException e) {
            return false;
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        BreventApplication application = (BreventApplication) getApplication();
        hideStopped(application);
        String action = intent.getAction();
        Notification notification = postNotification(application);
        UILog.d("show notification");
        startForeground(ID, notification);
        if (AppsDisabledFragment.hasRoot() && !checkPort()) {
            startBrevent(action);
        }
        if (!AppsDisabledFragment.hasRoot() || !checkPort()) {
            showStopped(application);
        }
        UILog.d("hide notification");
        stopForeground(true);
    }

    private void startBrevent(String action) {
        BreventApplication application = (BreventApplication) getApplication();
        if (BreventIntent.ACTION_RUN_AS_ROOT.equalsIgnoreCase(action)) {
            UILog.d("startBreventSync, action: " + action);
            application.notifyRootCompleted(startBreventSync());
        } else if (application.allowRoot()) {
            UILog.d("startBrevent, action: " + action);
            startBrevent();
        }
    }

    private void sleep(int s) {
        try {
            Thread.sleep(1000 * s);
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
            try {
                if (BreventProtocol.checkPortSync()) {
                    UILog.d("checked");
                    for (int i = 0; i < ADB_TIMEOUT; ++i) {
                        if (future.isDone()) {
                            return Collections.emptyList();
                        }
                        sleep(1);
                    }
                    future.cancel(false);
                    return Collections.emptyList();
                }
            } catch (IOException e) {
                // do nothing
            }
        } while (System.currentTimeMillis() < timeout);
        try {
            future.get(1, TimeUnit.SECONDS);
            return results;
        } catch (InterruptedException | ExecutionException e) {
            String msg = "(Cannot start Brevent)";
            UILog.d(msg, e);
            return Collections.singletonList(msg);
        } catch (TimeoutException e) {
            String msg = "(Cannot start Brevent in " + TIMEOUT + " seconds)";
            UILog.d(msg, e);
            return Collections.singletonList(msg);
        } finally {
            if (!future.isDone()) {
                future.cancel(false);
            }
        }
    }

    private List<String> startBreventSync() {
        BreventApplication application = (BreventApplication) getApplication();
        String path = application.copyBrevent();
        if (path == null) {
            return Collections.singletonList("(Cannot make brevent)");
        } else if (BuildConfig.ADB_K != null) {
            return startBreventAdb(path);
        } else {
            return startBreventRoot(path);
        }
    }

    private List<String> startBreventAdb(String path) {
        boolean needClose = false;
        boolean needStop = false;
        boolean success = false;
        String port = SystemProperties.get("service.adb.tcp.port", "");
        UILog.d("service.adb.tcp.port: " + port);
        if (TextUtils.isEmpty(port) || !TextUtils.isDigitsOnly(port)) {
            needClose = true;
            needStop = !AppsDisabledFragment.isAdbRunning();
            su("setprop service.adb.tcp.port 5555; setprop ctl.restart adbd");
            port = SystemProperties.get("service.adb.tcp.port", "");
            if (!"5555".equals(port)) {
                return Collections.singletonList("(Cannot network adb)");
            }
            sleep(1);
        }
        UILog.d("adb port: " + port);
        makeSureKeys();
        String message = "(cannot adb)";
        for (int i = 0; i < ADB_TIMEOUT; ++i) {
            try {
                message = new SimpleAdb(Integer.parseInt(port), path).run();
                success = true;
                break;
            } catch (IOException e) {
                UILog.w("cannot adb(" + e.getMessage() + ")", e);
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                message = sw.toString();
            }
            sleep(1);
        }
        if (success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            su(FIXO);
        }
        if (needClose) {
            String command = needStop ? "setprop ctl.stop adbd" : "setprop ctl.restart adbd";
            su("setprop service.adb.tcp.port -1; " + command);
        }
        if (success) {
            return Collections.singletonList(message);
        } else {
            List<String> messages = new ArrayList<>();
            messages.add(message);
            messages.add(System.lineSeparator());
            messages.addAll(startBreventRoot(path));
            return messages;
        }
    }

    private synchronized List<String> su(String command) {
        String prefix = "[SU] " + command;
        UILog.d(prefix);
        List<String> result = Shell.SU.run(command);
        if (result == null) {
            UILog.d(prefix + ": (no output)");
        } else {
            for (String s : result) {
                UILog.d(prefix + ": " + s);
            }
        }
        return result;
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
        su(command);
        return true;
    }

    private List<String> startBreventRoot(String path) {
        UILog.d("startBrevent: $SHELL " + path);
        List<String> results = su("$SHELL " + path);
        if (results == null) {
            results = su(path);
        }
        if (results == null) {
            results = Collections.singletonList("(cannot root)");
        }
        for (String result : results) {
            UILog.d(result);
        }
        return results;
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

    private static boolean allowRoot(BreventApplication application, String action) {
        boolean allowRoot = application.allowRoot();
        UILog.d("action: " + action + ", allowRoot: " + allowRoot);
        return allowRoot && AppsDisabledFragment.hasRoot();
    }

    private static boolean shouldForeground() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static void checkBrevent(BreventApplication application) {
        try {
            if (application.checkPort(true)) {
                UILog.d("brevent worked");
            } else {
                showNoBrevent(application, true);
            }
        } catch (NetworkErrorException e) {
            UILog.w("brevent checking timeout");
        }
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
