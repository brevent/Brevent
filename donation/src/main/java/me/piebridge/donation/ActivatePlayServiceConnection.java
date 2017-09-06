package me.piebridge.donation;

import android.os.Looper;

/**
 * Created by thom on 2017/9/7.
 */
public class ActivatePlayServiceConnection extends PlayServiceConnection {

    public ActivatePlayServiceConnection(Looper looper, DonateActivity donateActivity) {
        super(MESSAGE_ACTIVATE, looper, donateActivity);
    }

}