package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2018/2/25.
 */
public class BreventDisabledPackagesRequest extends BaseBreventProtocol {

    public final int uid;

    public BreventDisabledPackagesRequest(int uid) {
        super(DISABLED_PACKAGES_REQUEST);
        this.uid = uid;
    }

    BreventDisabledPackagesRequest(Parcel in) {
        super(in);
        uid = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(uid);
    }

    @Override
    public String toString() {
        return super.toString() + ", uid: " + uid;
    }

}
