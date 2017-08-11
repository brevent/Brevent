package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/6/26.
 */
public class BreventDisableRoot extends BreventProtocol {


    public final long mDaemonTime;

    public final long mServerTime;

    public final boolean mSupportStandby;

    public final boolean mSupportUpgrade;

    public BreventDisableRoot(long daemonTime, long serverTime, boolean supportStandby,
                              boolean supportUpgrade) {
        super(BreventProtocol.SHOW_ROOT);
        mDaemonTime = daemonTime;
        mServerTime = serverTime;
        mSupportStandby = supportStandby;
        mSupportUpgrade = supportUpgrade;
    }

    BreventDisableRoot(Parcel parcel) {
        super(parcel);
        mDaemonTime = parcel.readLong();
        mServerTime = parcel.readLong();
        mSupportStandby = parcel.readInt() != 0;
        mSupportUpgrade = parcel.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mDaemonTime);
        dest.writeLong(mServerTime);
        dest.writeInt(mSupportStandby ? 1 : 0);
        dest.writeInt(mSupportUpgrade ? 1 : 0);
    }

}
