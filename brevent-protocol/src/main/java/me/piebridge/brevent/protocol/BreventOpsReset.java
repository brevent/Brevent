package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/10/22.
 */
public class BreventOpsReset extends BreventProtocol {

    public final String packageName;

    public BreventOpsReset(String packageName) {
        super(OPS_RESET);
        this.packageName = packageName;
    }

    BreventOpsReset(Parcel in) {
        super(in);
        this.packageName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(packageName);
    }

}
