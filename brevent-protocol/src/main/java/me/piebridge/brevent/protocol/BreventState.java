package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2018/2/18.
 */
public class BreventState extends BreventProtocol {

    public final boolean enable;

    public final boolean launcher;

    public final boolean launch;

    public final String packageName;

    public BreventState(boolean enable, boolean launcher, boolean launch, String packageName) {
        super(UPDATE_STATE);
        this.enable = enable;
        this.launcher = launcher;
        this.launch = launch;
        this.packageName = packageName;
    }

    BreventState(Parcel in) {
        super(in);
        enable = in.readInt() != 0;
        launcher = in.readInt() != 0;
        launch = in.readInt() != 0;
        packageName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(enable ? 1 : 0);
        dest.writeInt(launcher ? 1 : 0);
        dest.writeInt(launch ? 1 : 0);
        dest.writeString(packageName);
    }

    @Override
    public String toString() {
        return super.toString() + ", enable: " + enable + ", launcher: " + launcher
                + ", packageName: " + packageName;
    }

}
