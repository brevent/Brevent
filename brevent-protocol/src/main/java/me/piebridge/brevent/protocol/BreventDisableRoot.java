package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/6/26.
 */
public class BreventDisableRoot extends BreventProtocol {


    public final long mDaemonTime;

    public final long mServerTime;

    public final boolean mSupportStandby;

    public final boolean mSupportStopped;

    public BreventDisableRoot(long daemonTime, long serverTime, boolean supportStandby,
                              boolean supportStopped) {
        super(BreventProtocol.SHOW_ROOT);
        mDaemonTime = daemonTime;
        mServerTime = serverTime;
        mSupportStandby = supportStandby;
        mSupportStopped = supportStopped;
    }

    BreventDisableRoot(Parcel parcel) {
        super(parcel);
        mDaemonTime = parcel.readLong();
        mServerTime = parcel.readLong();
        mSupportStandby = parcel.readInt() != 0;
        mSupportStopped = parcel.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mDaemonTime);
        dest.writeLong(mServerTime);
        dest.writeInt(mSupportStandby ? 1 : 0);
        dest.writeInt(mSupportStopped ? 1 : 0);
    }

}
