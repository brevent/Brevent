package me.piebridge.donation;

import android.os.Looper;

/**
 * Created by thom on 2017/9/7.
 */
class ActivatePlayServiceConnection extends PlayServiceConnection {

    ActivatePlayServiceConnection(Looper looper, DonateActivity donateActivity) {
        super(MESSAGE_ACTIVATE, looper, donateActivity);
    }

}