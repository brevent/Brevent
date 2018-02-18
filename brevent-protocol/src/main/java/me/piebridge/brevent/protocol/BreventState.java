package me.piebridge.brevent.protocol;

import android.os.Parcel;

import java.util.Collection;

/**
 * Created by thom on 2018/2/18.
 */
public class BreventState extends BreventProtocol {

    public final boolean enable;

    public final Collection<String> packageNames;

    public BreventState(boolean enable, Collection<String> packageNames) {
        super(UPDATE_STATE);
        this.enable = enable;
        this.packageNames = packageNames;
    }

    BreventState(Parcel in) {
        super(in);
        enable = in.readInt() != 0;
        packageNames = ParcelUtils.readCollection(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(enable ? 1 : 0);
        ParcelUtils.writeCollection(dest, packageNames);
    }

    @Override
    public String toString() {
        return super.toString() + ", enable: " + enable + ", packageNames: " + packageNames;
    }

}
