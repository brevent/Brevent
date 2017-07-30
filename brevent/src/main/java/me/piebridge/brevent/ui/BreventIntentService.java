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
import android.preference.PreferenceManager;

import java.util.Collections;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventProtocol;

public class BreventIntentService extends IntentService {

    public static final int ID = 59526;

    private static final String CHANNEL_ID = "root";

    public BreventIntentService() {
        super("BreventIntentService");
    }

    private boolean checkPort() {
        try {
            return BreventProtocol.checkPort(UILog.TAG);
        } catch (NetworkErrorException e) {
            return false;
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Notification notification = postNotification(getApplicationContext());
        UILog.d("show notification");
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(ID, notification);
        if (shouldForeground()) {
            startForeground(ID, notification);
        }
        if (!checkPort()) {
            startBrevent(intent.getAction());
        }
        UILog.d("hide notification");
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(ID);
        stopSelf();
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) && !checkPort()) {
            showStopped(getApplicationContext());
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

    private List<String> startBrevent() {
        BreventApplication application = (BreventApplication) getApplication();
        String path = application.copyBrevent();
        if (path != null) {
            UILog.d("startBrevent: " + path);
            List<String> results = Shell.SU.run(path);
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
            if (channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_MAX) {
                nm.deleteNotificationChannel(CHANNEL_ID);
            }
            channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.brevent),
                    NotificationManager.IMPORTANCE_MAX);
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
        builder.setSmallIcon(R.drawable.ic_brevent_server);
        builder.setContentTitle(context.getString(R.string.brevent_status_starting));
        return builder.build();
    }

    private static void showStopped(Context context) {
        Notification.Builder builder = buildNotification(context);
        builder.setAutoCancel(true);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setSmallIcon(R.drawable.ic_brevent_server);
        builder.setContentTitle(context.getString(R.string.brevent_status_stopped));
        builder.setContentIntent(PendingIntent.getActivity(context, 0,
                new Intent(context, BreventActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        Notification notification = builder.build();
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(ID, notification);
    }

    public static void startBrevent(Context context, String action) {
        boolean allowRoot;
        try {
            allowRoot = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext())
                    .getBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, false);
            UILog.d("action: " + action + ", allowRoot: " + allowRoot);
        } catch (IllegalStateException e) {
            allowRoot = true;
            UILog.d("action: " + action + ", allowRoot: (assume true)", e);
        }
        if ((BreventIntent.ACTION_RUN_AS_ROOT.equals(action) || allowRoot)) {
            Intent intent = new Intent(context, BreventIntentService.class);
            intent.setAction(action);
            if (shouldForeground()) {
                UILog.d("will startForegroundService");
                context.startForegroundService(intent);
            } else {
                UILog.d("will startService");
                context.startService(intent);
            }
        } else {
            showStopped(context);
        }
    }

    private static boolean shouldForeground() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

}
