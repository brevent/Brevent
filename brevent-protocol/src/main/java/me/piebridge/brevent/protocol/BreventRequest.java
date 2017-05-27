package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/5/25.
 */
public class BreventRequest extends BreventProtocol {

    public BreventRequest() {
        super(BreventProtocol.STATUS_REQUEST);
    }

    BreventRequest(Parcel in) {
        super(in);
    }

}
