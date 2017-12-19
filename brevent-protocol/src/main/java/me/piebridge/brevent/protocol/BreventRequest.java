package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/5/25.
 */
public class BreventRequest extends BreventProtocol {

    public final boolean check;

    public BreventRequest(boolean check) {
        super(BreventProtocol.STATUS_REQUEST);
        this.check = check;
    }

    BreventRequest(Parcel in) {
        super(in);
        check = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(check ? 1 : 0);
    }

}
