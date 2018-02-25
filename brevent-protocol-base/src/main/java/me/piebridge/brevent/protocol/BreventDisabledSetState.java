package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2018/2/25.
 */
public class BreventDisabledSetState extends BaseBreventProtocol {

    public final String packageName;

    public final int uid;

    public final boolean enable;

    public BreventDisabledSetState(String packageName, int uid, boolean enable) {
        super(DISABLED_SET_STATE);
        this.packageName = packageName;
        this.enable = enable;
        this.uid = uid;
    }

    BreventDisabledSetState(Parcel in) {
        super(in);
        packageName = in.readString();
        uid = in.readInt();
        enable = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(packageName);
        dest.writeInt(uid);
        dest.writeInt(enable ? 1 : 0);
    }

    @Override
    public String toString() {
        return super.toString() + ", packageName: " + packageName + ", uid: " + uid
                + ", enable: " + enable;
    }

}
