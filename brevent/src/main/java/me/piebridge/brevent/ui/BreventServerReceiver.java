package me.piebridge.brevent.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventIntent;

/**
 * Created by thom on 2017/4/19.
 */
public class BreventServerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BreventIntent.ACTION_HOME_TID.equals(action)) {
            int homeTid = intent.getIntExtra(BreventIntent.EXTRA_HOME_TID, 0);
            if (homeTid > 0) {
                String message = context.getResources().getString(R.string.toast_home_tid, homeTid);
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        }
    }

}
