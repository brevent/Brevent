package me.piebridge.donation;

import android.os.Looper;

/**
 * Created by thom on 2017/9/7.
 */
class DonatePlayServiceConnection extends PlayServiceConnection {

    DonatePlayServiceConnection(Looper looper, DonateActivity donateActivity) {
        super(MESSAGE_DONATE, looper, donateActivity);
    }

}