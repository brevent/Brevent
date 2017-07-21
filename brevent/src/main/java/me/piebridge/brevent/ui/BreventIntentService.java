package me.piebridge.brevent.ui;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventIntent;

public class BreventIntentService extends IntentService {

    public static final int ID = 59526;

    private static final String CHANNEL_ID = "root";

    public BreventIntentService() {
        super("BreventIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        postNotification(action);
        BreventApplication application = (BreventApplication) getApplication();
        UILog.d("onHandleIntent, action: " + action + ", started: " + application.started);
        boolean runAsRoot = BreventIntent.ACTION_RUN_AS_ROOT.equalsIgnoreCase(action);
        if (!runAsRoot && !application.started && Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            application.started = AppsActivityHandler.checkPort();
        }
        if (!application.started || runAsRoot) {
            application.started = true;
            startBrevent();
            if (runAsRoot) {
                application.notifyRootCompleted();
            }
        }
        hideNotification();
        stopSelf();
    }

    private boolean startBrevent() {
        BreventApplication application = (BreventApplication) getApplication();
        String path = application.copyBrevent();
        if (path != null) {
            UILog.d("startBrevent: sh " + path);
            List<String> results = Shell.SU.run("sh " + path);
            if (results != null) {
                for (String result : results) {
                    UILog.d(result);
                }
                return true;
            }
        }
        ((BreventApplication) getApplication()).started = false;
        return false;
    }

    private void hideNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(ID);
    }

    @SuppressWarnings("deprecation")
    private Notification.Builder buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
            if (channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_LOW) {
                nm.deleteNotificationChannel(CHANNEL_ID);
            }
            channel = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
            return new Notification.Builder(this, CHANNEL_ID);
        } else {
            return new Notification.Builder(this);
        }
    }

    private void postNotification(String action) {
        Notification.Builder builder = buildNotification();
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_brevent_server);
        Notification notification = builder.build();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(ID, notification);
        if (shouldForeground(action)) {
            startForeground(ID, notification);
        }
    }

    public static void startBrevent(Context context, String action) {
        Intent intent = new Intent(context, BreventIntentService.class);
        intent.setAction(action);
        if (shouldForeground(action)) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private static boolean shouldForeground(String action) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !BreventIntent.ACTION_RUN_AS_ROOT.equalsIgnoreCase(action);
    }

}
