package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/6/26.
 */
public class BreventDisableRoot extends BreventProtocol {

    public BreventDisableRoot() {
        super(BreventProtocol.STATUS_DISABLE_ROOT);
    }

    BreventDisableRoot(Parcel parcel) {
        super(parcel);
    }

}
