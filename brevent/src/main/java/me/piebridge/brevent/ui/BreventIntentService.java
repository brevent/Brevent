package me.piebridge.brevent.ui;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventIntent;

public class BreventIntentService extends IntentService {

    public static final int ID = 59526;

    public BreventIntentService() {
        super("BreventIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        BreventApplication application = (BreventApplication) getApplication();
        String action = intent.getAction();
        UILog.d("onHandleIntent, action: " + action + ", started: " + application.started);
        if (!application.started || BreventIntent.ACTION_RUN_AS_ROOT.equals(action)) {
            application.started = true;
            startBrevent();
            hideNotification();
        }
    }

    private boolean startBrevent() {
        BreventApplication application = (BreventApplication) getApplication();
        String path = application.copyBrevent();
        if (path != null) {
            postNotification();
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

    private void postNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_brevent_server);
        Notification notification = builder.build();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(ID, notification);
    }

    public static void startBrevent(Context context, String action) {
        Intent intent = new Intent(context, BreventIntentService.class);
        intent.setAction(action);
        context.startService(intent);
    }

}
