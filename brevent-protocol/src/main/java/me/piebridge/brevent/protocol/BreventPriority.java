package me.piebridge.brevent.protocol;

import android.os.Parcel;

import java.util.Collection;

/**
 * brevent list
 * <p>
 * Created by thom on 2017/2/6.
 */
public class BreventPriority extends BreventProtocol {

    public boolean priority = false;

    public Collection<String> packageNames;

    public BreventPriority(boolean priority, Collection<String> packageNames) {
        super(UPDATE_PRIORITY);
        this.priority = priority;
        this.packageNames = packageNames;
    }

    BreventPriority(Parcel in) {
        super(in);
        priority = in.readInt() != 0;
        packageNames = ParcelUtils.readCollection(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(priority ? 1 : 0);
        ParcelUtils.writeCollection(dest, packageNames);
    }

    @Override
    public String toString() {
        return super.toString() + ", priority: " + priority + ", packageNames: " + packageNames;
    }

}
