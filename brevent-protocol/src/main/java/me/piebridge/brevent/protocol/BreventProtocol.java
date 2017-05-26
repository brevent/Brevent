package me.piebridge.brevent.protocol;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * brevent protocol, request via socket, response via broadcast
 * <p>
 * Created by thom on 2017/2/6.
 */
public abstract class BreventProtocol implements Parcelable {

    public static final InetAddress HOST = InetAddress.getLoopbackAddress();

    // md5(BuildConfig.APPLICATION_ID)
    public static final int PORT = 59526;

    private static final int VERSION = BuildConfig.VERSION_CODE;

    public static final int STATUS_REQUEST = 0;
    public static final int STATUS_RESPONSE = 1;
    public static final int UPDATE_BREVENT = 2;
    public static final int CONFIGURATION = 3;
    public static final int UPDATE_PRIORITY = 4;

    private int mVersion;

    private int mAction;

    public boolean retry;

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

    public final boolean versionMismatched() {
        return mVersion != VERSION;
    }

    public void writeTo(DataOutputStream os) throws IOException {
        Parcel parcel = Parcel.obtain();
        writeToParcel(parcel, 0);
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

    public static BreventProtocol readFrom(DataInputStream is) throws IOException {
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

        return unwrap(uncompress(bytes));
    }

    private String getActionName(int action) {
        switch (action) {
            case STATUS_REQUEST:
                return "request";
            case STATUS_RESPONSE:
                return "response";
            case UPDATE_BREVENT:
                return "brevent";
            case CONFIGURATION:
                return "configuration";
            case UPDATE_PRIORITY:
                return "priority";
            default:
                return "(unknown: " + action + ")";
        }
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
                creator = BreventRequest.CREATOR;
                break;
            case STATUS_RESPONSE:
                creator = BreventResponse.CREATOR;
                break;
            case UPDATE_BREVENT:
                creator = BreventPackages.CREATOR;
                break;
            case CONFIGURATION:
                creator = BreventConfiguration.CREATOR;
                break;
            case UPDATE_PRIORITY:
                creator = BreventPriority.CREATOR;
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

        public IOTooLargeException(int size) {
            super();
            mSize = size;
        }

        public int getSize() {
            return mSize;
        }

    }

}
