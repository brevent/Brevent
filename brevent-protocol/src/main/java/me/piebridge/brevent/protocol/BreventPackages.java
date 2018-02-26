package me.piebridge.brevent.protocol;

import android.os.Parcel;

import java.util.Collection;

/**
 * brevent list
 * <p>
 * Created by thom on 2017/2/6.
 */
public class BreventPackages extends BreventProtocol {

    public boolean brevent = false;

    public boolean undoable = false;

    public Collection<String> packageNames;

    public BreventPackages(boolean brevent, Collection<String> packageNames) {
        super(UPDATE_BREVENT);
        this.brevent = brevent;
        this.packageNames = packageNames;
    }

    BreventPackages(Parcel in) {
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
                + ", packageNames: " + packageNames;
    }

}
