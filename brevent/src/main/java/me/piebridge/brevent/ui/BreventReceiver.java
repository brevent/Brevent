package me.piebridge.brevent.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.piebridge.brevent.protocol.BreventIntent;

public class BreventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        UILog.d("received: " + action);
        Context applicationContext = LocaleUtils.updateResources(context).getApplicationContext();
        if (applicationContext instanceof BreventApplication) {
            BreventApplication application = (BreventApplication) applicationContext;
            if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                    || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                BreventIntentService.startBrevent(application, action);
            } else if (BreventIntent.ACTION_ALARM.equals(action)) {
                BreventIntentService.checkBrevent(application);
            }
        }
    }

}
