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
public final class BreventPackages extends BreventToken implements Parcelable {

    public boolean brevent = false;

    public boolean undoable = false;

    public Collection<String> packageNames;

    public BreventPackages(boolean brevent, UUID uuid, Collection<String> packageNames) {
        super(UPDATE_BREVENT, uuid);
        this.brevent = brevent;
        this.packageNames = packageNames;
    }

    protected BreventPackages(Parcel in) {
        super(in);
        brevent = in.readInt() != 0;
        undoable = in.readInt() != 0;
        packageNames = ParcelUtils.readCollection(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(brevent ? 1 : 0);
        dest.writeInt(undoable ? 1 : 0);
        ParcelUtils.writeCollection(dest, packageNames);
    }

    public void undo() {
        undoable = false;
        brevent = !brevent;
    }

    @Override
    public String toString() {
        return super.toString() + ", brevent: " + brevent + ", undoable: " + undoable
                + ", brevent: " + brevent;
    }

    public static final Creator<BreventPackages> CREATOR = new Creator<BreventPackages>() {
        @Override
        public BreventPackages createFromParcel(Parcel in) {
            return new BreventPackages(in);
        }

        @Override
        public BreventPackages[] newArray(int size) {
            return new BreventPackages[size];
        }
    };

}
