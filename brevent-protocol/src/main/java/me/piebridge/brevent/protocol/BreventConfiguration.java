package me.piebridge.brevent.protocol;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.text.TextUtils;

import java.io.PrintWriter;

/**
 * configuration
 * <p>
 * Created by thom on 2017/2/6.
 */
public class BreventConfiguration extends BreventProtocol {

    public static final String BREVENT_AUTO_UPDATE = "brevent_auto_update";
    public static final boolean DEFAULT_BREVENT_AUTO_UPDATE = true;

    public static final String BREVENT_TIMEOUT = "brevent_timeout";
    public static final int DEFAULT_BREVENT_TIMEOUT = 1800;
    public static final int MIN_BREVENT_TIMEOUT = 60;

    public static final String BREVENT_ALLOW_ROOT = "brevent_allow_root";
    public static final boolean DEFAULT_BREVENT_ALLOW_ROOT = false;

    public static final String BREVENT_OPTIMIZE_VPN = "brevent_optimize_vpn";
    public static final boolean DEFAULT_BREVENT_OPTIMIZE_VPN = false;

    public static final String BREVENT_METHOD = "brevent_method";
    public static final int BREVENT_METHOD_STANDBY_FORCE_STOP = 1;
    public static final int BREVENT_METHOD_STANDBY_ONLY = 2;
    public static final int BREVENT_METHOD_FORCE_STOP_ONLY = 3;
    public static final int DEFAULT_BREVENT_METHOD = BREVENT_METHOD_STANDBY_FORCE_STOP;

    public static final String BREVENT_STANDBY_TIMEOUT = "brevent_standby_timeout";
    public static final int DEFAULT_BREVENT_STANDBY_TIMEOUT = 3600;
    public static final int MIN_BREVENT_STANDBY_TIMEOUT = 300;

    public static final String BREVENT_CHECK_NOTIFICATION = "brevent_check_notification";
    public static final boolean DEFAULT_BREVENT_CHECK_NOTIFICATION = true;

    public static final String BREVENT_AGGRESSIVE = "brevent_aggressive";
    public static final boolean DEFAULT_BREVENT_AGGRESSIVE = false;

    public static final String BREVENT_ABNORMAL_BACK = "brevent_abnormal_back";
    public static final boolean DEFAULT_BREVENT_ABNORMAL_BACK = false;

    public boolean autoUpdate = DEFAULT_BREVENT_AUTO_UPDATE;

    public int timeout = DEFAULT_BREVENT_TIMEOUT;

    public boolean allowRoot = DEFAULT_BREVENT_ALLOW_ROOT;

    public int method = DEFAULT_BREVENT_METHOD;

    public boolean optimizeVpn = DEFAULT_BREVENT_OPTIMIZE_VPN;

    public int standbyTimeout = DEFAULT_BREVENT_STANDBY_TIMEOUT;

    public boolean checkNotification = DEFAULT_BREVENT_CHECK_NOTIFICATION;

    public boolean aggressive = DEFAULT_BREVENT_AGGRESSIVE;

    public boolean abnormalBack = DEFAULT_BREVENT_ABNORMAL_BACK;

    public BreventConfiguration() {
        super(CONFIGURATION);
    }

    public BreventConfiguration(SharedPreferences sharedPreferences) {
        super(CONFIGURATION);
        autoUpdate = sharedPreferences.getBoolean(BREVENT_AUTO_UPDATE, DEFAULT_BREVENT_AUTO_UPDATE);
        setValue(BREVENT_TIMEOUT, sharedPreferences.getString(BREVENT_TIMEOUT,
                "" + DEFAULT_BREVENT_TIMEOUT));
        allowRoot = sharedPreferences.getBoolean(BREVENT_ALLOW_ROOT, DEFAULT_BREVENT_ALLOW_ROOT);
        method = convertMethod(sharedPreferences.getString(BREVENT_METHOD, ""));
        optimizeVpn = sharedPreferences.getBoolean(BREVENT_OPTIMIZE_VPN,
                DEFAULT_BREVENT_OPTIMIZE_VPN);
        setValue(BREVENT_STANDBY_TIMEOUT, sharedPreferences.getString(BREVENT_STANDBY_TIMEOUT,
                "" + DEFAULT_BREVENT_STANDBY_TIMEOUT));
        checkNotification = sharedPreferences.getBoolean(BREVENT_CHECK_NOTIFICATION,
                DEFAULT_BREVENT_CHECK_NOTIFICATION);
        aggressive = sharedPreferences.getBoolean(BREVENT_AGGRESSIVE,
                DEFAULT_BREVENT_AGGRESSIVE);
        abnormalBack = sharedPreferences.getBoolean(BREVENT_ABNORMAL_BACK,
                DEFAULT_BREVENT_ABNORMAL_BACK);
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

    BreventConfiguration(Parcel in) {
        super(in);
        autoUpdate = in.readInt() != 0;
        timeout = in.readInt();
        allowRoot = in.readInt() != 0;
        method = in.readInt();
        optimizeVpn = in.readInt() != 0;
        standbyTimeout = in.readInt();
        checkNotification = in.readInt() != 0;
        aggressive = in.readInt() != 0;
        abnormalBack = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(autoUpdate ? 1 : 0);
        dest.writeInt(timeout);
        dest.writeInt(allowRoot ? 1 : 0);
        dest.writeInt(method);
        dest.writeInt(optimizeVpn ? 1 : 0);
        dest.writeInt(standbyTimeout);
        dest.writeInt(checkNotification ? 1 : 0);
        dest.writeInt(aggressive ? 1 : 0);
        dest.writeInt(abnormalBack ? 1 : 0);
    }

    public void write(PrintWriter pw) {
        write(pw, BREVENT_AUTO_UPDATE, autoUpdate);
        write(pw, BREVENT_TIMEOUT, timeout);
        write(pw, BREVENT_ALLOW_ROOT, allowRoot);
        write(pw, BREVENT_METHOD, method);
        write(pw, BREVENT_OPTIMIZE_VPN, optimizeVpn);
        write(pw, BREVENT_STANDBY_TIMEOUT, standbyTimeout);
        write(pw, BREVENT_CHECK_NOTIFICATION, checkNotification);
        write(pw, BREVENT_AGGRESSIVE, aggressive);
        write(pw, BREVENT_ABNORMAL_BACK, abnormalBack);
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

    public void setValue(String key, String value) {
        switch (key) {
            case BREVENT_AUTO_UPDATE:
                autoUpdate = Boolean.parseBoolean(value);
                break;
            case BREVENT_TIMEOUT:
                if (isDigit(value, 0x7)) {
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
            case BREVENT_OPTIMIZE_VPN:
                optimizeVpn = Boolean.parseBoolean(value);
                break;
            case BREVENT_STANDBY_TIMEOUT:
                if (isDigit(value, 0x7)) {
                    standbyTimeout = Integer.parseInt(value);
                }
                if (standbyTimeout < MIN_BREVENT_STANDBY_TIMEOUT) {
                    standbyTimeout = MIN_BREVENT_STANDBY_TIMEOUT;
                }
                break;
            case BREVENT_CHECK_NOTIFICATION:
                checkNotification = Boolean.parseBoolean(value);
                break;
            case BREVENT_AGGRESSIVE:
                aggressive = Boolean.parseBoolean(value);
                break;
            case BREVENT_ABNORMAL_BACK:
                abnormalBack = Boolean.parseBoolean(value);
                break;
            default:
                break;
        }
    }

    private boolean isDigit(String value, int maxLength) {
        return !TextUtils.isEmpty(value) && TextUtils.isDigitsOnly(value) &&
                value.length() < maxLength;
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
        if (this.optimizeVpn != request.optimizeVpn) {
            this.optimizeVpn = request.optimizeVpn;
            updated = true;
        }
        if (this.standbyTimeout != request.standbyTimeout) {
            this.standbyTimeout = request.standbyTimeout;
            updated = true;
        }
        if (this.checkNotification != request.checkNotification) {
            this.checkNotification = request.checkNotification;
            updated = true;
        }
        if (this.aggressive != request.aggressive) {
            this.aggressive = request.aggressive;
            updated = true;
        }
        if (this.abnormalBack != request.abnormalBack) {
            this.abnormalBack = request.abnormalBack;
            updated = true;
        }
        return updated;
    }

    public boolean isForceStopOnly() {
        return method == BREVENT_METHOD_FORCE_STOP_ONLY;
    }

}
