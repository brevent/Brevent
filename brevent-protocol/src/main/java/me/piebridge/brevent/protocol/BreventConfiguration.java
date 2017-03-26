package me.piebridge.brevent.protocol;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.text.TextUtils;

import java.io.PrintWriter;
import java.util.UUID;

/**
 * configuration
 * <p>
 * Created by thom on 2017/2/6.
 */
public class BreventConfiguration extends BreventToken {

    public static final String BREVENT_AUTO_UPDATE = "brevent_auto_update";
    public static final boolean DEFAULT_BREVENT_AUTO_UPDATE = true;

    public static final String BREVENT_TIMEOUT = "brevent_timeout";
    public static final int DEFAULT_BREVENT_TIMEOUT = 1800;
    public static final int MIN_BREVENT_TIMEOUT = 60;

    public static final String BREVENT_ALLOW_ROOT = "brevent_allow_root";
    public static final boolean DEFAULT_BREVENT_ALLOW_ROOT = false;

    public static final String BREVENT_ALLOW_GCM = "brevent_allow_gcm";
    public static final boolean DEFAULT_BREVENT_ALLOW_GCM = false;

    public static final String BREVENT_METHOD = "brevent_method";
    public static final int BREVENT_METHOD_STANDBY_FORCE_STOP = 1;
    public static final int BREVENT_METHOD_STANDBY_ONLY = 2;
    public static final int BREVENT_METHOD_FORCE_STOP_ONLY = 3;
    public static final int DEFAULT_BREVENT_METHOD = BREVENT_METHOD_STANDBY_FORCE_STOP;

    public static final String BREVENT_STANDBY_TIMEOUT = "brevent_standby_timeout";
    public static final int DEFAULT_BREVENT_STANDBY_TIMEOUT = 3600;
    public static final int MIN_BREVENT_STANDBY_TIMEOUT = 900;

    public boolean autoUpdate = DEFAULT_BREVENT_AUTO_UPDATE;

    public int timeout = DEFAULT_BREVENT_TIMEOUT;

    public boolean allowRoot = DEFAULT_BREVENT_ALLOW_ROOT;

    public int method = DEFAULT_BREVENT_METHOD;

    public boolean allowGcm = DEFAULT_BREVENT_ALLOW_GCM;

    public int standbyTimeout = DEFAULT_BREVENT_STANDBY_TIMEOUT;

    public BreventConfiguration(UUID token, SharedPreferences sharedPreferences) {
        super(CONFIGURATION, token);
        autoUpdate = sharedPreferences.getBoolean(BREVENT_AUTO_UPDATE, DEFAULT_BREVENT_AUTO_UPDATE);
        setValue(BREVENT_TIMEOUT, sharedPreferences.getString(BREVENT_TIMEOUT, "" + DEFAULT_BREVENT_TIMEOUT));
        allowRoot = sharedPreferences.getBoolean(BREVENT_ALLOW_ROOT, DEFAULT_BREVENT_ALLOW_ROOT);
        method = convertMethod(sharedPreferences.getString(BREVENT_METHOD, ""));
        allowGcm = sharedPreferences.getBoolean(BREVENT_ALLOW_GCM, DEFAULT_BREVENT_ALLOW_GCM);
        setValue(BREVENT_STANDBY_TIMEOUT, sharedPreferences.getString(BREVENT_STANDBY_TIMEOUT, "" + DEFAULT_BREVENT_STANDBY_TIMEOUT));
    }

    private int convertMethod(String string) {
        switch (string) {
            case "standby_only":
            case "" + BREVENT_METHOD_STANDBY_ONLY:
                return BREVENT_METHOD_STANDBY_ONLY;
            case "forcestop_only":
            case "" + BREVENT_METHOD_FORCE_STOP_ONLY:
                return BREVENT_METHOD_FORCE_STOP_ONLY;
            case "standby":
            case "standby_forcestop":
            case "" + BREVENT_METHOD_STANDBY_FORCE_STOP:
            default:
                return BREVENT_METHOD_STANDBY_FORCE_STOP;
        }
    }

    protected BreventConfiguration(Parcel in) {
        super(in);
        autoUpdate = in.readInt() != 0;
        timeout = in.readInt();
        allowRoot = in.readInt() != 0;
        method = in.readInt();
        allowGcm = in.readInt() != 0;
        standbyTimeout = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(autoUpdate ? 1 : 0);
        dest.writeInt(timeout);
        dest.writeInt(allowRoot ? 1 : 0);
        dest.writeInt(method);
        dest.writeInt(allowGcm ? 1 : 0);
        dest.writeInt(standbyTimeout);
    }

    public void write(PrintWriter pw) {
        write(pw, BREVENT_AUTO_UPDATE, autoUpdate);
        write(pw, BREVENT_TIMEOUT, timeout);
        write(pw, BREVENT_ALLOW_ROOT, allowRoot);
        write(pw, BREVENT_METHOD, method);
        write(pw, BREVENT_ALLOW_GCM, allowGcm);
        write(pw, BREVENT_STANDBY_TIMEOUT, standbyTimeout);
    }

    private void write(PrintWriter pw, String key, int value) {
        pw.print(key);
        pw.print("=");
        pw.println(value);
    }

    private void write(PrintWriter pw, String key, boolean value) {
        pw.print(key);
        pw.print("=");
        pw.println(value);
    }

    public BreventConfiguration(UUID token) {
        super(CONFIGURATION, token);
    }

    public void setValue(String key, String value) {
        switch (key) {
            case BREVENT_AUTO_UPDATE:
                autoUpdate = Boolean.parseBoolean(value);
                break;
            case BREVENT_TIMEOUT:
                if (TextUtils.isDigitsOnly(value) && value.length() < 0x7) {
                    timeout = Integer.parseInt(value);
                }
                if (timeout < MIN_BREVENT_TIMEOUT) {
                    timeout = MIN_BREVENT_TIMEOUT;
                }
                break;
            case BREVENT_ALLOW_ROOT:
                allowRoot = Boolean.parseBoolean(value);
                break;
            case BREVENT_METHOD:
                method = convertMethod(value);
                break;
            case BREVENT_ALLOW_GCM:
                allowGcm = Boolean.parseBoolean(value);
                break;
            case BREVENT_STANDBY_TIMEOUT:
                if (TextUtils.isDigitsOnly(value) && value.length() < 0x7) {
                    standbyTimeout = Integer.parseInt(value);
                }
                if (standbyTimeout < MIN_BREVENT_STANDBY_TIMEOUT) {
                    standbyTimeout = MIN_BREVENT_STANDBY_TIMEOUT;
                }
                break;
            default:
                break;
        }
    }

    public boolean update(BreventConfiguration request) {
        boolean updated = false;
        if (this.autoUpdate != request.autoUpdate) {
            this.autoUpdate = request.autoUpdate;
            updated = true;
        }
        if (this.timeout != request.timeout) {
            this.timeout = request.timeout;
            updated = true;
        }
        if (this.allowRoot != request.allowRoot) {
            this.allowRoot = request.allowRoot;
            updated = true;
        }
        if (this.method != request.method) {
            this.method = request.method;
            updated = true;
        }
        if (this.allowGcm != request.allowGcm) {
            this.allowGcm = request.allowGcm;
            updated = true;
        }
        if (this.standbyTimeout != request.standbyTimeout) {
            this.standbyTimeout = request.standbyTimeout;
            updated = true;
        }
        return updated;
    }

    public static final Creator<BreventConfiguration> CREATOR = new Creator<BreventConfiguration>() {
        @Override
        public BreventConfiguration createFromParcel(Parcel in) {
            return new BreventConfiguration(in);
        }

        @Override
        public BreventConfiguration[] newArray(int size) {
            return new BreventConfiguration[size];
        }
    };

}
