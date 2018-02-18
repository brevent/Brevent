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
    public static final String BREVENT_ABNORMAL_BACK = "brevent_abnormal_back";
    public static final boolean DEFAULT_BREVENT_ABNORMAL_BACK = false;

    public static final String BREVENT_OPTIMIZE_AUDIO = "brevent_optimize_audio";
    public static final boolean DEFAULT_BREVENT_OPTIMIZE_AUDIO = false;

    public static final String BREVENT_CHECKING = "brevent_checking";
    public static final boolean DEFAULT_BREVENT_CHECKING = false;

    public static final String BREVENT_APPOPS = "brevent_appops";
    public static final boolean DEFAULT_BREVENT_APPOPS = false;

    public static final String BREVENT_DISABLE = "brevent_disable";
    public static final boolean DEFAULT_BREVENT_DISABLE = false;

    public static final String BREVENT_BACKGROUND = "brevent_background";
    public static final boolean DEFAULT_BREVENT_BACKGROUND = false;

    public boolean autoUpdate = DEFAULT_BREVENT_AUTO_UPDATE;

    public int timeout = DEFAULT_BREVENT_TIMEOUT;

    public int method = DEFAULT_BREVENT_METHOD;

    public int standbyTimeout = DEFAULT_BREVENT_STANDBY_TIMEOUT;

    public boolean checkNotification = DEFAULT_BREVENT_CHECK_NOTIFICATION;

    public boolean abnormalBack = DEFAULT_BREVENT_ABNORMAL_BACK;

    public long androidId;

    public boolean optimizeAudio = DEFAULT_BREVENT_OPTIMIZE_AUDIO;

    public boolean checking = DEFAULT_BREVENT_CHECKING;

    public boolean background = DEFAULT_BREVENT_BACKGROUND;

    public boolean appops = DEFAULT_BREVENT_APPOPS;

    public boolean disable = DEFAULT_BREVENT_DISABLE;

    public boolean xposed;

    public BreventConfiguration() {
        super(CONFIGURATION);
    }

    public BreventConfiguration(SharedPreferences sharedPreferences) {
        super(CONFIGURATION);
        autoUpdate = sharedPreferences.getBoolean(BREVENT_AUTO_UPDATE, DEFAULT_BREVENT_AUTO_UPDATE);
        setValue(BREVENT_TIMEOUT, sharedPreferences.getString(BREVENT_TIMEOUT,
                "" + DEFAULT_BREVENT_TIMEOUT));
        method = convertMethod(sharedPreferences.getString(BREVENT_METHOD, ""));
        setValue(BREVENT_STANDBY_TIMEOUT, sharedPreferences.getString(BREVENT_STANDBY_TIMEOUT,
                "" + DEFAULT_BREVENT_STANDBY_TIMEOUT));
        checkNotification = sharedPreferences.getBoolean(BREVENT_CHECK_NOTIFICATION,
                DEFAULT_BREVENT_CHECK_NOTIFICATION);
        abnormalBack = sharedPreferences.getBoolean(BREVENT_ABNORMAL_BACK,
                DEFAULT_BREVENT_ABNORMAL_BACK);
        optimizeAudio = sharedPreferences.getBoolean(BREVENT_OPTIMIZE_AUDIO,
                DEFAULT_BREVENT_OPTIMIZE_AUDIO);
        checking = sharedPreferences.getBoolean(BREVENT_CHECKING, DEFAULT_BREVENT_CHECKING);
        appops = sharedPreferences.getBoolean(BREVENT_APPOPS, DEFAULT_BREVENT_APPOPS);
        disable = sharedPreferences.getBoolean(BREVENT_DISABLE, DEFAULT_BREVENT_DISABLE);
        background = sharedPreferences.getBoolean(BREVENT_BACKGROUND, DEFAULT_BREVENT_BACKGROUND);
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
        method = in.readInt();
        standbyTimeout = in.readInt();
        checkNotification = in.readInt() != 0;
        abnormalBack = in.readInt() != 0;
        androidId = in.readLong();
        optimizeAudio = in.readInt() != 0;
        checking = in.readInt() != 0;
        appops = in.readInt() != 0;
        disable = in.readInt() != 0;
        background = in.readInt() != 0;
        xposed = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(autoUpdate ? 1 : 0);
        dest.writeInt(timeout);
        dest.writeInt(method);
        dest.writeInt(standbyTimeout);
        dest.writeInt(checkNotification ? 1 : 0);
        dest.writeInt(abnormalBack ? 1 : 0);
        dest.writeLong(androidId);
        dest.writeInt(optimizeAudio ? 1 : 0);
        dest.writeInt(checking ? 1 : 0);
        dest.writeInt(appops ? 1 : 0);
        dest.writeInt(disable ? 1 : 0);
        dest.writeInt(background ? 1 : 0);
        dest.writeInt(xposed ? 1 : 0);
    }

    public void write(PrintWriter pw) {
        write(pw, BREVENT_AUTO_UPDATE, autoUpdate);
        write(pw, BREVENT_TIMEOUT, timeout);
        write(pw, BREVENT_METHOD, method);
        write(pw, BREVENT_STANDBY_TIMEOUT, standbyTimeout);
        write(pw, BREVENT_CHECK_NOTIFICATION, checkNotification);
        write(pw, BREVENT_ABNORMAL_BACK, abnormalBack);
        write(pw, BREVENT_OPTIMIZE_AUDIO, optimizeAudio);
        write(pw, BREVENT_CHECKING, checking);
        write(pw, BREVENT_APPOPS, appops);
        write(pw, BREVENT_DISABLE, disable);
        write(pw, BREVENT_BACKGROUND, background);
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
                if (isDigit(value, 0x8)) {
                    timeout = Integer.parseInt(value);
                }
                if (timeout < MIN_BREVENT_TIMEOUT) {
                    timeout = MIN_BREVENT_TIMEOUT;
                }
                break;
            case BREVENT_METHOD:
                method = convertMethod(value);
                break;
            case BREVENT_STANDBY_TIMEOUT:
                if (isDigit(value, 0x6)) {
                    standbyTimeout = Integer.parseInt(value);
                }
                if (standbyTimeout < MIN_BREVENT_STANDBY_TIMEOUT) {
                    standbyTimeout = MIN_BREVENT_STANDBY_TIMEOUT;
                }
                break;
            case BREVENT_CHECK_NOTIFICATION:
                checkNotification = Boolean.parseBoolean(value);
                break;
            case BREVENT_ABNORMAL_BACK:
                abnormalBack = Boolean.parseBoolean(value);
                break;
            case BREVENT_OPTIMIZE_AUDIO:
                optimizeAudio = Boolean.parseBoolean(value);
                break;
            case BREVENT_CHECKING:
                checking = Boolean.parseBoolean(value);
                break;
            case BREVENT_APPOPS:
                appops = Boolean.parseBoolean(value);
                break;
            case BREVENT_DISABLE:
                disable = Boolean.parseBoolean(value);
                break;
            case BREVENT_BACKGROUND:
                background = Boolean.parseBoolean(value);
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
        if (this.method != request.method) {
            this.method = request.method;
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
        if (this.abnormalBack != request.abnormalBack) {
            this.abnormalBack = request.abnormalBack;
            updated = true;
        }
        if (this.optimizeAudio != request.optimizeAudio) {
            this.optimizeAudio = request.optimizeAudio;
            updated = true;
        }
        if (this.checking != request.checking) {
            this.checking = request.checking;
            updated = true;
        }
        if (this.appops != request.appops) {
            this.appops = request.appops;
            updated = true;
        }
        if (this.disable != request.disable) {
            this.disable = request.disable;
            updated = true;
        }
        if (this.background != request.background) {
            this.background = request.background;
            updated = true;
        }
        if (!this.xposed && request.xposed) {
            this.xposed = true;
        }
        return updated;
    }

    public boolean isForceStopOnly() {
        return method == BREVENT_METHOD_FORCE_STOP_ONLY;
    }

}
