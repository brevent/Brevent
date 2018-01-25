package me.piebridge.brevent.protocol;

import android.app.usage.UsageStats;
import android.content.pm.PackageInfo;
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

    public static final int PROCESS_STATE_AUDIO = -5;

    public static final int PROCESS_STATE_AUDIO_PAUSED = -6;

    public final Collection<String> mBrevent;

    public final Collection<String> mPriority;

    public final SimpleArrayMap<String, SparseIntArray> mProcesses;

    public final Collection<String> mTrustAgents;

    public final Collection<String> mPackages;

    public final boolean mSupportStopped;

    public final boolean mSupportStandby;

    public final boolean mSupportUpgrade;

    public final long mDaemonTime;

    public final long mServerTime;

    public final boolean mGranted;

    public final Collection<String> mAndroidProcesses;

    public final Collection<String> mFullPowerList;

    public final String mAlipaySum;

    public final String mVpn;

    public final boolean mSupportAppops;

    public final boolean mAlipaySin;

    public final boolean mForceStopped;

    public final Collection<PackageInfo> mInstantPackages;

    public final SimpleArrayMap<String, UsageStats> mStats;

    public BreventResponse(Collection<String> brevent, Collection<String> priority,
                           SimpleArrayMap<String, SparseIntArray> processes,
                           Collection<String> trustAgents, boolean supportStopped,
                           boolean supportStandby, long daemonTime, long serverTime, boolean granted,
                           Collection<String> androidProcesses,
                           Collection<String> fullPowerList, boolean supportUpgrade,
                           String alipaySum, String vpn, Collection<String> packages,
                           boolean supportAppops, boolean alipaySin,
                           boolean forceStopped, Collection<PackageInfo> instantPackages,
                           SimpleArrayMap<String, UsageStats> stats) {
        super(BreventProtocol.STATUS_RESPONSE);
        mBrevent = brevent;
        mPriority = priority;
        mProcesses = processes;
        mTrustAgents = trustAgents;
        mSupportStopped = supportStopped;
        mSupportStandby = supportStandby;
        mDaemonTime = daemonTime;
        mServerTime = serverTime;
        mGranted = granted;
        mAndroidProcesses = androidProcesses;
        mFullPowerList = fullPowerList;
        mSupportUpgrade = supportUpgrade;
        mAlipaySum = alipaySum;
        mVpn = vpn;
        mPackages = packages;
        mSupportAppops = supportAppops;
        mAlipaySin = alipaySin;
        mForceStopped = forceStopped;
        mInstantPackages = instantPackages;
        mStats = stats;
    }

    BreventResponse(Parcel in) {
        super(in);
        mBrevent = ParcelUtils.readCollection(in);
        mPriority = ParcelUtils.readCollection(in);
        mProcesses = ParcelUtils.readSparseIntArrayMap(in);
        mTrustAgents = ParcelUtils.readCollection(in);
        mSupportStopped = in.readInt() != 0;
        mSupportStandby = in.readInt() != 0;
        mDaemonTime = in.readLong();
        mServerTime = in.readLong();
        mGranted = in.readInt() != 0;
        mAndroidProcesses = ParcelUtils.readCollection(in);
        mFullPowerList = ParcelUtils.readCollection(in);
        mSupportUpgrade = in.readInt() != 0;
        mAlipaySum = in.readString();
        mVpn = in.readString();
        mPackages = ParcelUtils.readCollection(in);
        mSupportAppops = in.readInt() != 0;
        mAlipaySin = in.readInt() != 0;
        mForceStopped = in.readInt() != 0;
        mInstantPackages = ParcelUtils.readPackages(in);
        mStats = ParcelUtils.readUsageStatsMap(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        ParcelUtils.writeCollection(dest, mBrevent);
        ParcelUtils.writeCollection(dest, mPriority);
        ParcelUtils.writeSpareIntArrayMap(dest, mProcesses);
        ParcelUtils.writeCollection(dest, mTrustAgents);
        dest.writeInt(mSupportStopped ? 1 : 0);
        dest.writeInt(mSupportStandby ? 1 : 0);
        dest.writeLong(mDaemonTime);
        dest.writeLong(mServerTime);
        dest.writeInt(mGranted ? 1 : 0);
        ParcelUtils.writeCollection(dest, mAndroidProcesses);
        ParcelUtils.writeCollection(dest, mFullPowerList);
        dest.writeInt(mSupportUpgrade ? 1 : 0);
        dest.writeString(mAlipaySum);
        dest.writeString(mVpn);
        ParcelUtils.writeCollection(dest, mPackages);
        dest.writeInt(mSupportAppops ? 1 : 0);
        dest.writeInt(mAlipaySin ? 1 : 0);
        dest.writeInt(mForceStopped ? 1 : 0);
        ParcelUtils.writePackages(dest, mInstantPackages);
        ParcelUtils.writeUsageStatsMap(dest, mStats);
    }

    public static boolean isStandby(SparseIntArray status) {
        return status != null && status.get(PROCESS_STATE_IDLE, 0) != 0;
    }

    public static boolean isAudio(SparseIntArray status) {
        return status != null && status.get(PROCESS_STATE_AUDIO, 0) != 0;
    }

    public static boolean isAudioPaused(SparseIntArray status) {
        return status != null && status.get(PROCESS_STATE_AUDIO_PAUSED, 0) != 0;
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
