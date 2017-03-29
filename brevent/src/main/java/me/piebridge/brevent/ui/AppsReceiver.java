package me.piebridge.brevent.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventProtocol;

/**
 * Created by thom on 2017/2/3.
 */
public class AppsReceiver extends BroadcastReceiver {

    private final String mToken;

    private Handler mHandler;

    public AppsReceiver(Handler handler, String token) {
        mHandler = handler;
        mToken = token;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BreventIntent.ACTION_BREVENT.equals(action)) {
            setResultData(mToken);
            mHandler.obtainMessage(BreventActivity.MESSAGE_BREVENT_RESPONSE,
                    BreventProtocol.unwrap(intent)).sendToTarget();
        }
    }

}
