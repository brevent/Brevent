package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/5/25.
 */
public class BreventRequest extends BreventProtocol {

    public final boolean check;

    public final String token;

    public BreventRequest(boolean check) {
        this(check, "");
    }

    public BreventRequest(boolean check, String token) {
        super(BreventProtocol.STATUS_REQUEST);
        this.check = check;
        this.token = token;
    }

    BreventRequest(Parcel in) {
        super(in);
        check = in.readInt() != 0;
        token = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(check ? 1 : 0);
        dest.writeString(token);
    }

}
