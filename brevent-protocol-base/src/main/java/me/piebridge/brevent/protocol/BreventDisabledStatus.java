package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2018/2/25.
 */
public class BreventDisabledStatus extends BaseBreventProtocol {

    public final boolean enabled;

    public BreventDisabledStatus(boolean enabled) {
        super(DISABLED_STATUS);
        this.enabled = enabled;
    }

    BreventDisabledStatus(Parcel in) {
        super(in);
        enabled = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(enabled ? 1 : 0);
    }

}
