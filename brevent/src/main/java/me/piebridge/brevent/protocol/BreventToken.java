package me.piebridge.brevent.protocol;

import android.os.Parcel;

import java.util.UUID;

import me.piebridge.brevent.BuildConfig;

/**
 * token
 * <p>
 * Created by thom on 2017/2/6.
 */
public abstract class BreventToken extends BreventProtocol {

    public static final UUID EMPTY_TOKEN = new UUID(0, 0);

    private UUID mToken;

    BreventToken(int action, UUID token) {
        super(action);
        if (token == null) {
            mToken = EMPTY_TOKEN;
        } else {
            mToken = token;
        }
    }

    BreventToken(Parcel in) {
        super(in);
        mToken = new UUID(in.readLong(), in.readLong());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mToken.getMostSignificantBits());
        dest.writeLong(mToken.getLeastSignificantBits());
    }

    public final UUID getToken() {
        return mToken;
    }

    @Override
    public String toString() {
        if (BuildConfig.RELEASE) {
            return super.toString();
        } else {
            return super.toString() + ", token: " + mToken;
        }
    }

}
