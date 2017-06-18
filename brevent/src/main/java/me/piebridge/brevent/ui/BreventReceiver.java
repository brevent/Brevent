package me.piebridge.brevent.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import me.piebridge.brevent.protocol.BreventConfiguration;

public class BreventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
                startBrevent(context, action);
            }
        } else {
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                startBrevent(context, action);
            }
        }
    }

    private void startBrevent(Context context, String action) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, false)) {
            UILog.d("received: " + action);
            BreventIntentService.startBrevent(context, action);
        }
    }

}
