package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/5/25.
 */
public class BreventRequest extends BreventProtocol {

    public final boolean confirmed;

    public BreventRequest(boolean confirmed) {
        super(BreventProtocol.STATUS_REQUEST);
        this.confirmed = confirmed;
    }

    BreventRequest(Parcel in) {
        super(in);
        confirmed = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(confirmed ? 1 : 0);
    }

}
