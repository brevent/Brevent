package me.piebridge.brevent.protocol;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collection;
import java.util.UUID;

/**
 * brevent list
 * <p>
 * Created by thom on 2017/2/6.
 */
public final class BreventPriority extends BreventToken implements Parcelable {

    public boolean priority = false;

    public Collection<String> packageNames;

    public BreventPriority(boolean priority, UUID uuid, Collection<String> packageNames) {
        super(UPDATE_PRIORITY, uuid);
        this.priority = priority;
        this.packageNames = packageNames;
    }

    protected BreventPriority(Parcel in) {
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

    public static final Creator<BreventPriority> CREATOR = new Creator<BreventPriority>() {
        @Override
        public BreventPriority createFromParcel(Parcel in) {
            return new BreventPriority(in);
        }

        @Override
        public BreventPriority[] newArray(int size) {
            return new BreventPriority[size];
        }
    };

}
