package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/6/26.
 */
public class BreventNoEvent extends BreventProtocol {

    public BreventNoEvent() {
        super(BreventProtocol.STATUS_NO_EVENT);
    }

    BreventNoEvent(Parcel in) {
        super(in);
    }

}
