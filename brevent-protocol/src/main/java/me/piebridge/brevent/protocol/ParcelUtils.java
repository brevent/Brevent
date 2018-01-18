package me.piebridge.brevent.protocol;

import android.content.pm.PackageInfo;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.support.v4.util.SimpleArrayMap;
import android.util.SparseIntArray;

import java.util.Collection;

/**
 * parcel utils for Brevent Protocol
 * <p>
 * Created by thom on 2017/2/6.
 */
class ParcelUtils {

    private ParcelUtils() {

    }

    static void writeCollection(Parcel dest, @Nullable Collection<String> collection) {
        if (collection == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(collection.size());
            for (String s : collection) {
                dest.writeString(s);
            }
        }
    }

    @NonNull
    static Collection<String> readCollection(Parcel in) {
        int size = in.readInt();
        if (size <= 0) {
            return new ArraySet<>();
        }
        Collection<String> collection = new ArraySet<>(size);
        for (int i = 0; i < size; ++i) {
            collection.add(in.readString());
        }
        return collection;
    }

    @NonNull
    private static SparseIntArray readSparseIntArray(Parcel in) {
        int size = in.readInt();
        if (size < 0) {
            return new SparseIntArray(0);
        }
        SparseIntArray array = new SparseIntArray(size);
        for (int i = 0; i < size; ++i) {
            array.append(in.readInt(), in.readInt());
        }
        return array;
    }

    private static void writeSparseIntArray(Parcel dest, @Nullable SparseIntArray array) {
        if (array == null) {
            dest.writeInt(-1);
        } else {
            int size = array.size();
            dest.writeInt(size);
            for (int i = 0; i < size; ++i) {
                dest.writeInt(array.keyAt(i));
                dest.writeInt(array.valueAt(i));
            }
        }
    }

    @NonNull
    static SimpleArrayMap<String, SparseIntArray> readSimpleArrayMap(Parcel in) {
        int size = in.readInt();
        if (size <= 0) {
            return new SimpleArrayMap<>();
        }
        SimpleArrayMap<String, SparseIntArray> map = new SimpleArrayMap<>(size);
        for (int i = 0; i < size; ++i) {
            map.put(in.readString(), readSparseIntArray(in));
        }
        return map;
    }

    static void writeSimpleArrayMap(Parcel dest,
                                    @Nullable SimpleArrayMap<String, SparseIntArray> map) {
        if (map == null) {
            dest.writeInt(-1);
        } else {
            int size = map.size();
            dest.writeInt(size);
            for (int i = 0; i < size; ++i) {
                dest.writeString(map.keyAt(i));
                writeSparseIntArray(dest, map.valueAt(i));
            }
        }
    }

    static Collection<PackageInfo> readPackages(Parcel in) {
        int size = in.readInt();
        if (size <= 0) {
            return new ArraySet<>();
        }
        Collection<PackageInfo> collection = new ArraySet<>(size);
        for (int i = 0; i < size; ++i) {
            collection.add(PackageInfo.CREATOR.createFromParcel(in));
        }
        return collection;
    }

    static void writePackages(Parcel dest, Collection<PackageInfo> collection) {
        if (collection == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(collection.size());
            for (PackageInfo packageInfo : collection) {
                packageInfo.writeToParcel(dest, 0);
            }
        }
    }

}
