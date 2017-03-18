package me.piebridge.brevent.protocol;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;
import android.util.SparseIntArray;

import java.util.Collection;
import java.util.UUID;

import me.piebridge.brevent.override.HideApiOverride;

/**
 * status
 * <p>
 * Created by thom on 2017/2/5.
 */
public class BreventStatus extends BreventToken implements Parcelable {

    public static final int PROCESS_STATE_IDLE = -2;

    public static final int PROCESS_STATE_INACTIVE = -3;

    public static final int PROCESS_STATE_PERSISTENT = -4;

    private final Collection<String> mBrevent;

    private final SimpleArrayMap<String, SparseIntArray> mProcesses;

    public BreventStatus(UUID token, Collection<String> brevent, SimpleArrayMap<String, SparseIntArray> processes) {
        super(BreventProtocol.STATUS_RESPONSE, token);
        mBrevent = brevent;
        mProcesses = processes;
    }

    BreventStatus(Parcel in) {
        super(in);
        mBrevent = ParcelUtils.readCollection(in);
        mProcesses = ParcelUtils.readSimpleArrayMap(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        ParcelUtils.writeCollection(dest, mBrevent);
        ParcelUtils.writeSimpleArrayMap(dest, mProcesses);
    }

    public static final Creator<BreventStatus> CREATOR = new Creator<BreventStatus>() {
        @Override
        public BreventStatus createFromParcel(Parcel in) {
            return new BreventStatus(in);
        }

        @Override
        public BreventStatus[] newArray(int size) {
            return new BreventStatus[size];
        }
    };

    @NonNull
    public SimpleArrayMap<String, SparseIntArray> getProcesses() {
        return mProcesses;
    }

    public Collection<String> getBrevent() {
        return mBrevent;
    }

    public static boolean isStandby(SparseIntArray status) {
        return status != null && status.get(PROCESS_STATE_IDLE, 0) != 0;
    }

    public static boolean isRunning(SparseIntArray status) {
        if (status == null) {
            return false;
        }
        int size = status.size();
        for (int i = 0; i < size; ++i) {
            int processState = status.keyAt(i);
            if (isProcess(processState)) {
                return true;
            }
        }
        return false;
    }

    public static int getInactive(SparseIntArray status) {
        if (status == null) {
            return 0;
        } else {
            return status.get(PROCESS_STATE_INACTIVE, 0);
        }
    }

    public static boolean isPersistent(SparseIntArray status) {
        return status != null && status.get(PROCESS_STATE_PERSISTENT, 0) != 0;
    }

    public static boolean isProcess(int processState) {
        return processState >= 0;
    }

    public static boolean isService(int processState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return HideApiOverride.isService(processState);
        } else {
            return HideApiOverride.isServiceL(processState);
        }
    }

    public static boolean isTop(int processState) {
        return HideApiOverride.isTop(processState);
    }

    public static boolean isCached(int processState) {
        return HideApiOverride.isCached(processState);
    }

    public static boolean isService(@NonNull SparseIntArray status) {
        int size = status.size();
        for (int i = 0; i < size; ++i) {
            int processState = status.keyAt(i);
            if (isService(processState)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isForegroundService(@NonNull SparseIntArray status) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int size = status.size();
            for (int i = 0; i < size; ++i) {
                int processState = status.keyAt(i);
                if (HideApiOverride.isForegroundService(processState)) {
                    return true;
                }
            }
        }
        return false;
    }

}
