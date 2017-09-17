package me.piebridge.brevent.ui;

import android.accounts.NetworkErrorException;
import android.app.Application;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import eu.chainfire.libsuperuser.Shell;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventProtocol;

public class BreventIntentService extends IntentService {

    public static final int ID = 59526;

    public static final int ID2 = 59527;

    private static final String CHANNEL_ID = "root";

    private static final int TIMEOUT = 15;

    private static final int CHECK_TIMEOUT_MS = 15_000;

    private ExecutorService executor = new ScheduledThreadPoolExecutor(0x1);

    private Future<List<String>> future;

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
        Notification notification = postNotification(getApplication());
        UILog.d("show notification");
        startForeground(ID, notification);
        if (!checkPort()) {
            startBrevent(intent.getAction());
        }
        UILog.d("hide notification");
        stopForeground(true);
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) && !checkPort()) {
            showStopped(getApplication());
        }
    }

    private void startBrevent(String action) {
        UILog.d("startBrevent, action: " + action);
        boolean runAsRoot = BreventIntent.ACTION_RUN_AS_ROOT.equalsIgnoreCase(action);
        List<String> output = startBrevent();
        if (runAsRoot) {
            BreventApplication application = (BreventApplication) getApplication();
            application.notifyRootCompleted(output);
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
        future = executor.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return startBreventSync();
            }
        });
        long timeout = System.currentTimeMillis() + CHECK_TIMEOUT_MS;
        do {
            sleep(1);
            try {
                if (BreventProtocol.checkPortSync()) {
                    UILog.d("checked");
                    if (!future.isDone()) {
                        future.cancel(false);
                    }
                    sleep(3);
                    return Collections.singletonList("(Brevent server started)");
                }
            } catch (IOException e) {
                // do nothing
            }
        } while (System.currentTimeMillis() < timeout);
        try {
            return future.get(1, TimeUnit.SECONDS);
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
        if (path != null) {
            UILog.d("startBrevent: $SHELL " + path);
            List<String> results = Shell.SU.run("$SHELL " + path);
            if (results == null) {
                UILog.d("startBrevent: " + path);
                results = Shell.SU.run(path);
            }
            if (results != null) {
                for (String result : results) {
                    UILog.d(result);
                }
                return results;
            }
            UILog.w("(no output)");
        }
        return Collections.emptyList();
    }

    private static Notification.Builder buildNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
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
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(ID, notification);
    }

    public static void startBrevent(Application application, String action) {
        if (BreventIntent.ACTION_RUN_AS_ROOT.equals(action) || allowRoot(application, action)) {
            Intent intent = new Intent(application, BreventIntentService.class);
            intent.setAction(action);
            if (shouldForeground()) {
                UILog.d("will startForegroundService");
                application.startForegroundService(intent);
            } else {
                UILog.d("will startService");
                application.startService(intent);
            }
        } else {
            showStopped(application);
        }
    }

    private static boolean allowRoot(Application application, String action) {
        boolean allowRoot = PreferencesUtils.getDevicePreferences(application)
                .getBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, true);
        if (allowRoot) {
            allowRoot = BreventApplication.allowRoot(application);
        }
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
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(ID2, notification);
        if (exit) {
            BreventActivity.cancelAlarm(context);
        }
    }

}
