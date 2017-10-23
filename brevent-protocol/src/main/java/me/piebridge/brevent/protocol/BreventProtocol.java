package me.piebridge.brevent.protocol;

import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Parcel;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import me.piebridge.brevent.override.HideApiOverrideM;

/**
 * brevent protocol, request via socket, response via broadcast
 * <p>
 * Created by thom on 2017/2/6.
 */
public abstract class BreventProtocol {

    // md5(BuildConfig.APPLICATION_ID)
    public static final int PORT = 59526;

    private static final int VERSION = BuildConfig.VERSION_CODE;

    public static final int STATUS_REQUEST = 0;
    public static final int STATUS_RESPONSE = 1;
    public static final int UPDATE_BREVENT = 2;
    public static final int CONFIGURATION = 3;
    public static final int UPDATE_PRIORITY = 4;
    public static final int STATUS_NO_EVENT = 5;
    public static final int SHOW_ROOT = 6;
    public static final int STATUS_OK = 7;
    public static final int STATUS_KO = 8;
    public static final int OPS_RESET = 9;
    public static final int OPS_UPDATE = 10;

    private int mVersion;

    private int mAction;

    public boolean retry;

    public BreventProtocol(int action) {
        this.mVersion = VERSION;
        this.mAction = action;
    }

    BreventProtocol(Parcel in) {
        mVersion = in.readInt();
        mAction = in.readInt();
    }

    @CallSuper
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mVersion);
        dest.writeInt(mAction);
    }

    public final int getAction() {
        return mAction;
    }

    public final boolean versionMismatched() {
        return mVersion != VERSION;
    }

    public static void writeTo(BreventProtocol protocol, DataOutputStream os) throws IOException {
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
    public static BreventProtocol readFrom(DataInputStream is) throws IOException {
        int size = is.readUnsignedShort();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return BreventOK.INSTANCE;
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
            case STATUS_NO_EVENT:
                return "no_event";
            case SHOW_ROOT:
                return "show_root";
            case STATUS_OK:
                return "status_ok";
            case STATUS_KO:
                return "status_ko";
            case OPS_RESET:
                return "ops_reset";
            case OPS_UPDATE:
                return "ops_update";
            default:
                return "(unknown: " + action + ")";
        }
    }

    private static BreventProtocol unwrap(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        parcel.readInt(); // skip version
        int action = parcel.readInt();
        parcel.setDataPosition(0);

        try {
            switch (action) {
                case STATUS_REQUEST:
                    return new BreventRequest(parcel);
                case STATUS_RESPONSE:
                    return new BreventResponse(parcel);
                case UPDATE_BREVENT:
                    return new BreventPackages(parcel);
                case CONFIGURATION:
                    return new BreventConfiguration(parcel);
                case UPDATE_PRIORITY:
                    return new BreventPriority(parcel);
                case STATUS_NO_EVENT:
                    return new BreventNoEvent(parcel);
                case SHOW_ROOT:
                    return new BreventDisableRoot(parcel);
                case STATUS_OK:
                    return BreventOK.INSTANCE;
                case STATUS_KO:
                    return BreventKO.INSTANCE;
                case OPS_RESET:
                    return new BreventOpsReset(parcel);
                case OPS_UPDATE:
                    return new BreventOpsUpdate(parcel);
                default:
                    return null;
            }
        } finally {
            parcel.recycle();
        }
    }

    private static byte[] compress(byte[] bytes) {
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

    private static byte[] uncompress(byte[] compressed) {
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

    @WorkerThread
    public static boolean checkPortSync() throws IOException {
        try (
                Socket socket = new Socket(InetAddress.getLoopbackAddress(), BreventProtocol.PORT);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                DataInputStream is = new DataInputStream(socket.getInputStream())
        ) {
            os.writeShort(0);
            os.flush();
            return BreventProtocol.readFrom(is) == BreventOK.INSTANCE;
        }
    }

    public static byte[] sha1(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(bytes);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static byte[] getFingerprint(String sourceDir) {
        Signature[] signatures = getSignatures(sourceDir);
        if (signatures == null || signatures.length != 0x1) {
            return null;
        }
        return sha1(signatures[0].toByteArray());
    }

    public static Signature[] getSignatures(String sourceDir) {
        try {
            PackageParser.Package pkg = new PackageParser.Package(sourceDir);
            pkg.baseCodePath = sourceDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageParser.collectCertificates(pkg, PackageManager.GET_SIGNATURES);
            } else {
                HideApiOverrideM.collectCertificates(pkg, PackageManager.GET_SIGNATURES);
            }
            return pkg.mSignatures;
        } catch (PackageParser.PackageParserException e) {
            // do nothing
            return null;
        }
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
