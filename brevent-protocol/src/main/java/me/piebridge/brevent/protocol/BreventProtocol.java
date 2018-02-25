package me.piebridge.brevent.protocol;

import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import me.piebridge.brevent.override.HideApiOverrideM;

/**
 * brevent protocol, request via socket, response via broadcast
 * <p>
 * Created by thom on 2017/2/6.
 */
public abstract class BreventProtocol extends BaseBreventProtocol {

    public static final InetAddress HOST = BaseBreventProtocol.HOST;

    public static final int PORT = BaseBreventProtocol.PORT;

    public static final int VERSION = BaseBreventProtocol.VERSION;

    public static final int STATUS_REQUEST = 0;
    public static final int STATUS_RESPONSE = 1;
    public static final int UPDATE_BREVENT = 2;
    public static final int CONFIGURATION = 3;
    public static final int UPDATE_PRIORITY = 4;
    public static final int STATUS_NO_EVENT = 5;
    public static final int UPDATE_STATE = 6;
    public static final int STATUS_OK = 7;
    public static final int OPS_KO = 8;
    public static final int OPS_RESET = 9;
    public static final int OPS_UPDATE = 10;
    public static final int OPS_OK = 11;
    public static final int CMD_REQUEST = 12;
    public static final int CMD_RESPONSE = 13;

    public BreventProtocol(int action) {
        super(action);
    }

    BreventProtocol(Parcel in) {
        super(in);
    }

    @Nullable
    public static BreventProtocol readFrom(DataInputStream is) throws IOException {
        BaseBreventProtocol baseProtocol = readFromBase(is);
        if (baseProtocol instanceof BreventProtocol) {
            return (BreventProtocol) baseProtocol;
        } else {
            return null;
        }
    }

    @Nullable
    public static BaseBreventProtocol readFromBase(DataInputStream is) throws IOException {
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

    protected String getActionName(int action) {
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
            case UPDATE_STATE:
                return "state";
            case STATUS_OK:
                return "status_ok";
            case OPS_KO:
                return "ops_ko";
            case OPS_RESET:
                return "ops_reset";
            case OPS_UPDATE:
                return "ops_update";
            case OPS_OK:
                return "ops_ok";
            case CMD_REQUEST:
                return "cmd_request";
            case CMD_RESPONSE:
                return "cmd_response";
            default:
                return "(unknown: " + action + ")";
        }
    }

    public static BaseBreventProtocol unwrap(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        parcel.readInt(); // skip version
        int action = parcel.readInt();
        parcel.setDataPosition(0);
        try {
            return unwrap(action, parcel);
        } finally {
            parcel.recycle();
        }
    }

    public static BaseBreventProtocol unwrap(int action, Parcel parcel) {
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
            case UPDATE_STATE:
                return new BreventState(parcel);
            case OPS_KO:
                return BreventOpsKO.INSTANCE;
            case OPS_RESET:
                return new BreventOpsReset(parcel);
            case OPS_UPDATE:
                return new BreventOpsUpdate(parcel);
            case OPS_OK:
                return BreventOpsOK.INSTANCE;
            case CMD_REQUEST:
                return new BreventCmdRequest(parcel);
            case CMD_RESPONSE:
                return new BreventCmdResponse(parcel);
            default:
                return BaseBreventProtocol.unwrapBase(action, parcel);
        }
    }

    @WorkerThread
    public static boolean checkPortSync() throws IOException {
        try (
                Socket socket = new Socket(BreventProtocol.HOST, BreventProtocol.PORT);
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

    public static long getStatsStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -0x5);
        return calendar.getTimeInMillis();
    }

}
