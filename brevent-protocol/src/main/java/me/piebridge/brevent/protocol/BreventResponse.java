package me.piebridge.brevent.protocol;

import android.app.usage.UsageStats;
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

    public static final int SUPPORT_UPGRADE = 1 << 0;
    public static final int SUPPORT_STOPPED = 1 << 1;
    public static final int SUPPORT_STANDBY = 1 << 2;
    public static final int SUPPORT_APPOPS = 1 << 3;
    public static final int SUPPORT_DISABLE = 1 << 4;
    public static final int SUPPORT_CHECK = 1 << 5;
    public static final int SUPPORT_GRANTED = 1 << 6;
    public static final int SUPPORT_SINGLE = 1 << 7;
    public static final int SUPPORT_EVENT = 1 << 8;

    public static final int PROCESS_STATE_IDLE = -2;

    public static final int PROCESS_STATE_INACTIVE = -3;

    public static final int PROCESS_STATE_PERSISTENT = -4;

    public static final int PROCESS_STATE_AUDIO = -5;

    public static final int PROCESS_STATE_AUDIO_PAUSED = -6;

    public final Collection<String> mPackages;
    public final Collection<BreventPackageInfo> mInstantPackages;
    public final Collection<String> mDisabledPackages;
    public final Collection<String> mDisabledLaunchers;
    public final Collection<String> mDisablingPackages;

    public final Collection<String> mBrevent;
    public final Collection<String> mPriority;

    public final String mVpn;
    public final SimpleArrayMap<String, BreventUsageStats> mStats;
    public final SimpleArrayMap<String, SparseIntArray> mProcesses;
    public final Collection<String> mTrustAgents;
    public final Collection<String> mCoreApps;
    public final Collection<String> mFullPowerList;

    public final long mDaemonTime;
    public final long mServerTime;
    public final long mEventTime;
    public final int mSupport;
    public final String mAlipaySum;

    public final boolean mSupportUpgrade;
    public final boolean mSupportStopped;
    public final boolean mSupportStandby;
    public final boolean mSupportAppops;
    public final boolean mSupportDisable;
    public final boolean mSupportCheck;
    public final boolean mSupportGranted;
    public final boolean mSupportSingle;
    public final boolean mSupportEvent;

    public BreventResponse(Collection<String> packages,
                           Collection<BreventPackageInfo> instantPackages,
                           Collection<String> disabledPackages,
                           Collection<String> disabledLaunchers,
                           Collection<String> disablingPackages,

                           Collection<String> brevent,
                           Collection<String> priority,

                           String vpn,
                           SimpleArrayMap<String, UsageStats> stats,
                           SimpleArrayMap<String, SparseIntArray> processes,
                           Collection<String> trustAgents,
                           Collection<String> coreApps,
                           Collection<String> fullPowerList,

                           long daemonTime, long serverTime, long eventTime,
                           int support, String alipaySum) {
        super(BreventProtocol.STATUS_RESPONSE);
        mPackages = packages;
        mInstantPackages = instantPackages;
        mDisabledPackages = disabledPackages;
        mDisabledLaunchers = disabledLaunchers;
        mDisablingPackages = disablingPackages;

        mBrevent = brevent;
        mPriority = priority;

        mVpn = vpn;
        mStats = BreventUsageStats.convert(stats);
        mProcesses = processes;
        mTrustAgents = trustAgents;
        mCoreApps = coreApps;
        mFullPowerList = fullPowerList;

        mDaemonTime = daemonTime;
        mServerTime = serverTime;
        mEventTime = eventTime;
        mSupport = support;
        mAlipaySum = alipaySum;

        mSupportUpgrade = hasSupport(mSupport, SUPPORT_UPGRADE);
        mSupportStopped = hasSupport(mSupport, SUPPORT_STOPPED);
        mSupportStandby = hasSupport(mSupport, SUPPORT_STANDBY);
        mSupportAppops = hasSupport(mSupport, SUPPORT_APPOPS);
        mSupportDisable = hasSupport(mSupport, SUPPORT_DISABLE);
        mSupportCheck = hasSupport(mSupport, SUPPORT_CHECK);
        mSupportGranted = hasSupport(mSupport, SUPPORT_GRANTED);
        mSupportSingle = hasSupport(mSupport, SUPPORT_SINGLE);
        mSupportEvent = hasSupport(mSupport, SUPPORT_EVENT);
    }

    private boolean hasSupport(int flags, int flag) {
        return (flags & flag) != 0;
    }

    BreventResponse(Parcel in) {
        super(in);
        mPackages = ParcelUtils.readCollection(in);
        mInstantPackages = ParcelUtils.readPackages(in);
        mDisabledPackages = ParcelUtils.readCollection(in);
        mDisabledLaunchers = ParcelUtils.readCollection(in);
        mDisablingPackages = ParcelUtils.readCollection(in);

        mBrevent = ParcelUtils.readCollection(in);
        mPriority = ParcelUtils.readCollection(in);

        mVpn = in.readString();
        mStats = ParcelUtils.readUsageStatsMap(in);
        mProcesses = ParcelUtils.readSparseIntArrayMap(in);
        mTrustAgents = ParcelUtils.readCollection(in);
        mCoreApps = ParcelUtils.readCollection(in);
        mFullPowerList = ParcelUtils.readCollection(in);

        mDaemonTime = in.readLong();
        mServerTime = in.readLong();
        mEventTime = in.readLong();
        mSupport = in.readInt();
        mAlipaySum = in.readString();

        mSupportUpgrade = hasSupport(mSupport, SUPPORT_UPGRADE);
        mSupportStopped = hasSupport(mSupport, SUPPORT_STOPPED);
        mSupportStandby = hasSupport(mSupport, SUPPORT_STANDBY);
        mSupportAppops = hasSupport(mSupport, SUPPORT_APPOPS);
        mSupportDisable = hasSupport(mSupport, SUPPORT_DISABLE);
        mSupportCheck = hasSupport(mSupport, SUPPORT_CHECK);
        mSupportGranted = hasSupport(mSupport, SUPPORT_GRANTED);
        mSupportSingle = hasSupport(mSupport, SUPPORT_SINGLE);
        mSupportEvent = hasSupport(mSupport, SUPPORT_EVENT);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        ParcelUtils.writeCollection(dest, mPackages);
        ParcelUtils.writePackages(dest, mInstantPackages);
        ParcelUtils.writeCollection(dest, mDisabledPackages);
        ParcelUtils.writeCollection(dest, mDisabledLaunchers);
        ParcelUtils.writeCollection(dest, mDisablingPackages);

        ParcelUtils.writeCollection(dest, mBrevent);
        ParcelUtils.writeCollection(dest, mPriority);

        dest.writeString(mVpn);
        ParcelUtils.writeUsageStatsMap(dest, mStats);
        ParcelUtils.writeSpareIntArrayMap(dest, mProcesses);
        ParcelUtils.writeCollection(dest, mTrustAgents);
        ParcelUtils.writeCollection(dest, mCoreApps);
        ParcelUtils.writeCollection(dest, mFullPowerList);

        dest.writeLong(mDaemonTime);
        dest.writeLong(mServerTime);
        dest.writeLong(mEventTime);
        dest.writeInt(mSupport);
        dest.writeString(mAlipaySum);
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
