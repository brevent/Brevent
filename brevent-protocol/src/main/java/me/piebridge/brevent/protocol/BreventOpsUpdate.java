package me.piebridge.brevent.protocol;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;

import java.util.Collection;

/**
 * Created by thom on 2017/10/22.
 */
public class BreventOpsUpdate extends BreventProtocol {

    public int mode;

    public String packageName;

    public Collection<Integer> ops;

    public BreventOpsUpdate(int mode, String packageName, Collection<Integer> ops) {
        super(OPS_UPDATE);
        this.mode = mode;
        this.packageName = packageName;
        this.ops = ops;
    }

    BreventOpsUpdate(Parcel in) {
        super(in);
        mode = in.readInt();
        packageName = in.readString();
        ops = readCollection(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mode);
        dest.writeString(packageName);
        writeCollection(dest, ops);
    }

    @NonNull
    static Collection<Integer> readCollection(Parcel in) {
        int size = in.readInt();
        if (size <= 0) {
            return new ArraySet<>();
        }
        Collection<Integer> collection = new ArraySet<>(size);
        for (int i = 0; i < size; ++i) {
            collection.add(in.readInt());
        }
        return collection;
    }

    static void writeCollection(Parcel dest, @Nullable Collection<Integer> collection) {
        if (collection == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(collection.size());
            for (Integer s : collection) {
                dest.writeInt(s);
            }
        }
    }

}
