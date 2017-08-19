package me.piebridge.brevent.protocol;

import android.os.Build;
import android.os.Parcel;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;
import android.util.SparseIntArray;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import me.piebridge.brevent.override.HideApiOverride;

/**
 * status
 * <p>
 * Created by thom on 2017/2/5.
 */
public class BreventResponse extends BreventProtocol {

    public static final int PROCESS_STATE_IDLE = -2;

    public static final int PROCESS_STATE_INACTIVE = -3;

    public static final int PROCESS_STATE_PERSISTENT = -4;

    public final Collection<String> mBrevent;

    public final Collection<String> mPriority;

    public final SimpleArrayMap<String, SparseIntArray> mProcesses;

    public final Collection<String> mTrustAgents;

    public final boolean mSupportStopped;

    public final boolean mSupportStandby;

    public final boolean mSupportUpgrade;

    public final long mDaemonTime;

    public final long mServerTime;

    public final int mUid;

    public final Collection<String> mAndroidProcesses;

    public final Collection<String> mFullPowerList;

    public final String mAlipaySum;

    public final Collection<String> mAudio;

    public final String mVpn;

    public BreventResponse(Collection<String> brevent, Collection<String> priority,
                           SimpleArrayMap<String, SparseIntArray> processes,
                           Collection<String> trustAgents, boolean supportStopped,
                           boolean supportStandby, long daemonTime, long serverTime, int uid,
                           Collection<String> androidProcesses,
                           Collection<String> fullPowerList, boolean supportUpgrade,
                           String alipaySum, Collection<String> audio, String vpn) {
        super(BreventProtocol.STATUS_RESPONSE);
        mBrevent = brevent;
        mPriority = priority;
        mProcesses = processes;
        mTrustAgents = trustAgents;
        mSupportStopped = supportStopped;
        mSupportStandby = supportStandby;
        mDaemonTime = daemonTime;
        mServerTime = serverTime;
        mUid = uid;
        mAndroidProcesses = androidProcesses;
        mFullPowerList = fullPowerList;
        mSupportUpgrade = supportUpgrade;
        mAlipaySum = alipaySum;
        mAudio = audio;
        mVpn = vpn;
    }

    BreventResponse(Parcel in) {
        super(in);
        mBrevent = ParcelUtils.readCollection(in);
        mPriority = ParcelUtils.readCollection(in);
        mProcesses = ParcelUtils.readSimpleArrayMap(in);
        mTrustAgents = ParcelUtils.readCollection(in);
        mSupportStopped = in.readInt() != 0;
        mSupportStandby = in.readInt() != 0;
        mDaemonTime = in.readLong();
        mServerTime = in.readLong();
        mUid = in.readInt();
        mAndroidProcesses = ParcelUtils.readCollection(in);
        mFullPowerList = ParcelUtils.readCollection(in);
        mSupportUpgrade = in.readInt() != 0;
        mAlipaySum = in.readString();
        mAudio = ParcelUtils.readCollection(in);
        mVpn = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        ParcelUtils.writeCollection(dest, mBrevent);
        ParcelUtils.writeCollection(dest, mPriority);
        ParcelUtils.writeSimpleArrayMap(dest, mProcesses);
        ParcelUtils.writeCollection(dest, mTrustAgents);
        dest.writeInt(mSupportStopped ? 1 : 0);
        dest.writeInt(mSupportStandby ? 1 : 0);
        dest.writeLong(mDaemonTime);
        dest.writeLong(mServerTime);
        dest.writeInt(mUid);
        ParcelUtils.writeCollection(dest, mAndroidProcesses);
        ParcelUtils.writeCollection(dest, mFullPowerList);
        dest.writeInt(mSupportUpgrade ? 1 : 0);
        dest.writeString(mAlipaySum);
        ParcelUtils.writeCollection(dest, mAudio);
        dest.writeString(mVpn);
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

    public static boolean isSafe(@NonNull SparseIntArray status) {
        int size = status.size();
        for (int i = 0; i < size; ++i) {
            int processState = status.keyAt(i);
            if (BreventResponse.isProcess(processState) && !HideApiOverride.isSafe(processState)) {
                return false;
            }
        }
        return true;
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

    public static int now() {
        // i don't think can larger than Integer.MAX_VALUE, which is boot since 68 years
        return (int) TimeUnit.MILLISECONDS.toSeconds(SystemClock.elapsedRealtime());
    }

}
