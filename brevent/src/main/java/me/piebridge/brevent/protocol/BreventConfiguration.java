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

    public static final String BREVENT_MOVE_BACK = "brevent_move_back";
    public static final boolean DEFAULT_BREVENT_MOVE_BACK = true;

    public static final String BREVENT_BACK_HOME = "brevent_back_home";
    public static final boolean DEFAULT_BREVENT_BACK_HOME = true;

    public static final String BREVENT_TIMEOUT = "brevent_timeout";
    public static final int DEFAULT_BREVENT_TIMEOUT = 1800;

    public static final String BREVENT_APPOPS_BACKGROUND = "brevent_appops_background";
    public static final boolean DEFAULT_BREVENT_APPOPS_BACKGROUND = false;

    public static final String BREVENT_MODE = "brevent_mode";

    public static final int BREVENT_MODE_LATER = 0;

    public static final int BREVENT_MODE_IMMEDIATE = 1;

    public boolean autoUpdate = DEFAULT_BREVENT_AUTO_UPDATE;

    public boolean backMove = DEFAULT_BREVENT_MOVE_BACK;

    public boolean backHome = DEFAULT_BREVENT_BACK_HOME;

    public int timeout = DEFAULT_BREVENT_TIMEOUT;

    public boolean appopsBackground = DEFAULT_BREVENT_APPOPS_BACKGROUND;

    public int mode = BREVENT_MODE_LATER;

    public BreventConfiguration(UUID token, SharedPreferences sharedPreferences) {
        super(CONFIGURATION, token);
        autoUpdate = sharedPreferences.getBoolean(BREVENT_AUTO_UPDATE, DEFAULT_BREVENT_AUTO_UPDATE);
        backMove = sharedPreferences.getBoolean(BREVENT_MOVE_BACK, DEFAULT_BREVENT_MOVE_BACK);
        backHome = sharedPreferences.getBoolean(BREVENT_BACK_HOME, DEFAULT_BREVENT_BACK_HOME);
        setValue(BREVENT_TIMEOUT, sharedPreferences.getString(BREVENT_TIMEOUT, "" + DEFAULT_BREVENT_TIMEOUT));
        appopsBackground = sharedPreferences.getBoolean(BREVENT_APPOPS_BACKGROUND, DEFAULT_BREVENT_APPOPS_BACKGROUND);
        mode = convertMode(sharedPreferences.getString(BREVENT_MODE, ""));
    }

    private int convertMode(String string) {
        switch (string) {
            case "immediate":
            case "" + BREVENT_MODE_IMMEDIATE:
                return BREVENT_MODE_IMMEDIATE;
            case "later":
            case "" + BREVENT_MODE_LATER:
            default:
                return BREVENT_MODE_LATER;
        }
    }

    protected BreventConfiguration(Parcel in) {
        super(in);
        autoUpdate = in.readInt() != 0;
        backMove = in.readInt() != 0;
        backHome = in.readInt() != 0;
        timeout = in.readInt();
        appopsBackground = in.readInt() != 0;
        mode = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(autoUpdate ? 1 : 0);
        dest.writeInt(backMove ? 1 : 0);
        dest.writeInt(backHome ? 1 : 0);
        dest.writeInt(timeout);
        dest.writeInt(appopsBackground ? 1 : 0);
        dest.writeInt(mode);
    }

    public void write(PrintWriter pw) {
        write(pw, BREVENT_AUTO_UPDATE, autoUpdate);
        write(pw, BREVENT_MOVE_BACK, backMove);
        write(pw, BREVENT_BACK_HOME, backHome);
        write(pw, BREVENT_TIMEOUT, timeout);
        write(pw, BREVENT_APPOPS_BACKGROUND, appopsBackground);
        write(pw, BREVENT_MODE, mode);
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
            case BREVENT_MOVE_BACK:
                backMove = Boolean.parseBoolean(value);
                break;
            case BREVENT_BACK_HOME:
                backHome = Boolean.parseBoolean(value);
                break;
            case BREVENT_APPOPS_BACKGROUND:
                appopsBackground = Boolean.parseBoolean(value);
                break;
            case BREVENT_TIMEOUT:
                if (TextUtils.isDigitsOnly(value) && value.length() < 0x6) {
                    timeout = Integer.parseInt(value);
                }
                break;
            case BREVENT_MODE:
                mode = convertMode(value);
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
        if (this.backMove != request.backMove) {
            this.backMove = request.backMove;
            updated = true;
        }
        if (this.backHome != request.backHome) {
            this.backHome = request.backHome;
            updated = true;
        }
        if (this.timeout != request.timeout) {
            this.timeout = request.timeout;
            updated = true;
        }
        if (this.appopsBackground != request.appopsBackground) {
            this.appopsBackground = request.appopsBackground;
            updated = true;
        }
        if (this.mode != request.mode) {
            this.mode = request.mode;
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
