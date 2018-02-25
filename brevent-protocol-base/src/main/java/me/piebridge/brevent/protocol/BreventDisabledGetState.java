package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2018/2/25.
 */
public class BreventDisabledGetState extends BaseBreventProtocol {

    public final String packageName;

    public final int uid;

    public final boolean disabled;

    public BreventDisabledGetState(String packageName, int uid, boolean disabled) {
        super(DISABLED_GET_STATE);
        this.packageName = packageName;
        this.uid = uid;
        this.disabled = disabled;
    }

    BreventDisabledGetState(Parcel in) {
        super(in);
        packageName = in.readString();
        uid = in.readInt();
        disabled = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(packageName);
        dest.writeInt(uid);
        dest.writeInt(disabled ? 1 : 0);
    }

    @Override
    public String toString() {
        return super.toString() + ", packageName: " + packageName + ", uid: " + uid
                + ", disabled: " + disabled;
    }

}
