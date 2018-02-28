package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/5/25.
 */
public class BreventRequest extends BreventProtocol {

    public boolean check = false;

    public BreventRequest() {
        super(BreventProtocol.STATUS_REQUEST);
    }

    BreventRequest(Parcel in) {
        super(in);
        check = in.readInt() != 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(check ? 1 : 0);
    }

}
