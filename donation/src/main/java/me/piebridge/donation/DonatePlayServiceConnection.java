package me.piebridge.donation;

import android.os.Looper;

/**
 * Created by thom on 2017/9/7.
 */

public class DonatePlayServiceConnection extends PlayServiceConnection {

    public DonatePlayServiceConnection(Looper looper, DonateActivity donateActivity) {
        super(MESSAGE_DONATE, looper, donateActivity);
    }

}