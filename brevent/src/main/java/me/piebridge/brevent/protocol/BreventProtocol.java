package me.piebridge.brevent.protocol;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.support.annotation.CallSuper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import me.piebridge.brevent.BuildConfig;

/**
 * brevent protocol, request via socket, response via broadcast
 * <p>
 * Created by thom on 2017/2/6.
 */
public class BreventProtocol implements Parcelable {

    private static final String KEY_BREVENT = "brevent";

    public static final InetAddress HOST = InetAddress.getLoopbackAddress();

    // md5(BuildConfig.APPLICATION_ID)
    public static final int PORT = 59526;

    private static final int VERSION = BuildConfig.VERSION_CODE;

    public static final int STATUS_REQUEST = 0;
    public static final int STATUS_RESPONSE = 1;
    public static final int UPDATE_BREVENT = 2;
    public static final int CONFIGURATION = 3;

    private int mVersion;

    private int mAction;

    public BreventProtocol(int action) {
        this.mVersion = VERSION;
        this.mAction = action;
    }

    protected BreventProtocol(Parcel in) {
        mVersion = in.readInt();
        mAction = in.readInt();
    }

    @Override
    @CallSuper
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mVersion);
        dest.writeInt(mAction);
    }

    @Override
    public final int describeContents() {
        return 0;
    }

    public final int getAction() {
        return mAction;
    }

    public final boolean versionUnmatched() {
        return mVersion != VERSION;
    }

    public void writeTo(DataOutputStream os) throws IOException, RemoteException {
        Parcel parcel = Parcel.obtain();
        writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();

        int size = bytes.length;
        if (size > Short.MAX_VALUE) {
            throw new TransactionTooLargeException();
        }
        os.writeShort(size);
        os.write(bytes);
    }

    public static BreventProtocol unwrap(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        parcel.readInt(); // skip version
        int action = parcel.readInt();
        parcel.setDataPosition(0);

        Parcelable.Creator<? extends BreventProtocol> creator;
        switch (action) {
            case STATUS_REQUEST:
                creator = BreventProtocol.CREATOR;
                break;
            case STATUS_RESPONSE:
                creator = BreventStatus.CREATOR;
                break;
            case UPDATE_BREVENT:
                creator = BreventPackages.CREATOR;
                break;
            case CONFIGURATION:
                creator = BreventConfiguration.CREATOR;
                break;
            default:
                creator = null;
                break;
        }
        try {
            if (creator != null) {
                return creator.createFromParcel(parcel);
            } else {
                return null;
            }
        } finally {
            parcel.recycle();
        }
    }

    public static void wrap(Intent intent, BreventProtocol protocol) {
        intent.putExtra(KEY_BREVENT, protocol);
    }

    public static BreventProtocol unwrap(Intent intent) {
        return intent.getParcelableExtra(KEY_BREVENT);
    }

    @Override
    public String toString() {
        return "version: " + mVersion + ", action: " + mAction;
    }

    public static final Creator<BreventProtocol> CREATOR = new Creator<BreventProtocol>() {
        @Override
        public BreventProtocol createFromParcel(Parcel in) {
            return new BreventProtocol(in);
        }

        @Override
        public BreventProtocol[] newArray(int size) {
            return new BreventProtocol[size];
        }
    };

}
