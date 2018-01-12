package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/5/25.
 */
public class BreventRequest extends BreventProtocol {

    public final String token;

    public BreventRequest() {
        this("");
    }

    public BreventRequest(String token) {
        super(BreventProtocol.STATUS_REQUEST);
        this.token = token;
    }

    BreventRequest(Parcel in) {
        super(in);
        token = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(token);
    }

}
