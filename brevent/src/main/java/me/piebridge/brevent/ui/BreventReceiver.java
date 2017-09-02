package me.piebridge.brevent.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.piebridge.brevent.protocol.BreventIntent;

public class BreventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        UILog.d("received: " + action);
        Context context = LocaleUtils.updateResources(c);
        if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            BreventIntentService.startBrevent(context, action);
        } else if (BreventIntent.ACTION_ALARM.equals(action)) {
            BreventIntentService.checkBrevent(context);
        }
    }

}
