package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2017/6/26.
 */
public class BreventNoEvent extends BreventProtocol {

    public boolean mExit;

    public BreventNoEvent(boolean exit) {
        super(BreventProtocol.STATUS_NO_EVENT);
        mExit = exit;
    }

    BreventNoEvent(Parcel in) {
        super(in);
        mExit = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mExit ? 1 : 0);
    }

}
