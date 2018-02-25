package me.piebridge.brevent.protocol;

import android.os.Parcel;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import me.piebridge.brevent.protocol.base.BuildConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * abstract brevent protocol
 * <p>
 * Created by thom on 2018/2/25.
 */
public abstract class BaseBreventProtocol {

    public static final InetAddress HOST = InetAddress.getLoopbackAddress();

    // md5(me.piebridge.brevent)
    public static final int PORT = 59526;

    public static final int VERSION = BuildConfig.VERSION_CODE;

    public static final int DISABLED_STATUS = 100;
    public static final int DISABLED_PACKAGES_REQUEST = 101;
    public static final int DISABLED_PACKAGES_RESPONSE = 102;
    public static final int DISABLED_GET_STATE = 103;
    public static final int DISABLED_SET_STATE = 104;

    private int mVersion;

    private int mAction;

    public boolean retry;

    public String token;

    public BaseBreventProtocol(int action) {
        this.mVersion = VERSION;
        this.mAction = action;
        this.token = "";
    }

    BaseBreventProtocol(Parcel in) {
        mVersion = in.readInt();
        mAction = in.readInt();
        token = in.readString();
    }

    @CallSuper
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mVersion);
        dest.writeInt(mAction);
        dest.writeString(token);
    }

    public final int getAction() {
        return mAction;
    }

    public final boolean versionMismatched() {
        return mVersion != VERSION;
    }

    public static void writeTo(BaseBreventProtocol protocol, DataOutputStream os)
            throws IOException {
        Parcel parcel = Parcel.obtain();
        protocol.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();

        bytes = compress(bytes);
        int size = bytes.length;
        if (size > 0xffff) {
            throw new IOTooLargeException(size);
        }
        os.writeShort(size);
        os.write(bytes);
    }

    @Nullable
    public static BaseBreventProtocol readFromBase(DataInputStream is) throws IOException {
        int size = is.readUnsignedShort();
        if (size == 0) {
            return null;
        }

        byte[] bytes = new byte[size];
        int length;
        int offset = 0;
        int remain = bytes.length;
        while (remain > 0 && (length = is.read(bytes, offset, remain)) != -1) {
            if (length > 0) {
                offset += length;
                remain -= length;
            }
        }

        return unwrapBase(uncompress(bytes));
    }

    protected String getActionName(int action) {
        switch (action) {
            case DISABLED_STATUS:
                return "disabled_status";
            case DISABLED_PACKAGES_REQUEST:
                return "disabled_packages_request";
            case DISABLED_PACKAGES_RESPONSE:
                return "disabled_packages_response";
            case DISABLED_GET_STATE:
                return "disabled_get_state";
            case DISABLED_SET_STATE:
                return "disabled_set_state";
            default:
                return "(unknown: " + action + ")";
        }
    }

    public static BaseBreventProtocol unwrapBase(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        parcel.readInt(); // skip version
        int action = parcel.readInt();
        parcel.setDataPosition(0);
        try {
            return unwrapBase(action, parcel);
        } finally {
            parcel.recycle();
        }
    }

    public static BaseBreventProtocol unwrapBase(int action, Parcel parcel) {
        switch (action) {
            case DISABLED_STATUS:
                return new BreventDisabledStatus(parcel);
            case DISABLED_PACKAGES_REQUEST:
                return new BreventDisabledPackagesRequest(parcel);
            case DISABLED_PACKAGES_RESPONSE:
                return new BreventDisabledPackagesResponse(parcel);
            case DISABLED_GET_STATE:
                return new BreventDisabledGetState(parcel);
            case DISABLED_SET_STATE:
                return new BreventDisabledSetState(parcel);
            default:
                return null;
        }
    }

    public static byte[] compress(byte[] bytes) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(baos);
            gos.write(bytes);
            gos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] uncompress(byte[] compressed) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            GZIPInputStream gis = new GZIPInputStream(bais);
            byte[] buffer = new byte[0x1000];
            int length;
            while ((length = gis.read(buffer)) != -1) {
                if (length > 0) {
                    baos.write(buffer, 0, length);
                }
            }
            gis.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "version: " + mVersion + ", action: " + getActionName(mAction);
    }

    public static class IOTooLargeException extends IOException {

        private final int mSize;

        IOTooLargeException(int size) {
            super();
            mSize = size;
        }

        public int getSize() {
            return mSize;
        }

    }

}
