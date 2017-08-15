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

    public final String mAlipaySum;

    public BreventDisableRoot(long daemonTime, long serverTime, boolean supportStandby,
                              boolean supportUpgrade, String alipaySum) {
        super(BreventProtocol.SHOW_ROOT);
        mDaemonTime = daemonTime;
        mServerTime = serverTime;
        mSupportStandby = supportStandby;
        mSupportUpgrade = supportUpgrade;
        mAlipaySum = alipaySum;
    }

    BreventDisableRoot(Parcel parcel) {
        super(parcel);
        mDaemonTime = parcel.readLong();
        mServerTime = parcel.readLong();
        mSupportStandby = parcel.readInt() != 0;
        mSupportUpgrade = parcel.readInt() != 0;
        mAlipaySum = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mDaemonTime);
        dest.writeLong(mServerTime);
        dest.writeInt(mSupportStandby ? 1 : 0);
        dest.writeInt(mSupportUpgrade ? 1 : 0);
        dest.writeString(mAlipaySum);
    }

}
