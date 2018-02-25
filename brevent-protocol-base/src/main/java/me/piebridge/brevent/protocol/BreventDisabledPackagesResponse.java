package me.piebridge.brevent.protocol;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thom on 2018/2/25.
 */
public class BreventDisabledPackagesResponse extends BaseBreventProtocol {

    public final List<String> packageNames;

    public BreventDisabledPackagesResponse(List<String> packageNames) {
        super(DISABLED_PACKAGES_RESPONSE);
        this.packageNames = packageNames;
    }

    BreventDisabledPackagesResponse(Parcel in) {
        super(in);
        int size = in.readInt();
        packageNames = size < 0 ? null : new ArrayList<>(size);
        if (packageNames != null) {
            in.readStringList(packageNames);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        if (packageNames == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(packageNames.size());
            dest.writeStringList(packageNames);
        }
    }

    @Override
    public String toString() {
        return super.toString() + ", packageNames: " + packageNames;
    }

}
