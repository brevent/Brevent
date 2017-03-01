package android.app;

import java.util.List;

/**
 * Created by thom on 2016/11/24.
 */
public class AppOpsManager {

    /** @hide No operation specified. */
    public static int OP_NONE = -1;
    /** @hide Access to coarse location information. */
    public static int OP_COARSE_LOCATION = 0;
    /** @hide Access to fine location information. */
    public static int OP_FINE_LOCATION = 1;
    /** @hide Causing GPS to run. */
    public static int OP_GPS = 2;
    /** @hide */
    public static int OP_VIBRATE = 3;
    /** @hide */
    public static int OP_READ_CONTACTS = 4;
    /** @hide */
    public static int OP_WRITE_CONTACTS = 5;
    /** @hide */
    public static int OP_READ_CALL_LOG = 6;
    /** @hide */
    public static int OP_WRITE_CALL_LOG = 7;
    /** @hide */
    public static int OP_READ_CALENDAR = 8;
    /** @hide */
    public static int OP_WRITE_CALENDAR = 9;
    /** @hide */
    public static int OP_WIFI_SCAN = 10;
    /** @hide */
    public static int OP_POST_NOTIFICATION = 11;
    /** @hide */
    public static int OP_NEIGHBORING_CELLS = 12;
    /** @hide */
    public static int OP_CALL_PHONE = 13;
    /** @hide */
    public static int OP_READ_SMS = 14;
    /** @hide */
    public static int OP_WRITE_SMS = 15;
    /** @hide */
    public static int OP_RECEIVE_SMS = 16;
    /** @hide */
    public static int OP_RECEIVE_EMERGECY_SMS = 17;
    /** @hide */
    public static int OP_RECEIVE_MMS = 18;
    /** @hide */
    public static int OP_RECEIVE_WAP_PUSH = 19;
    /** @hide */
    public static int OP_SEND_SMS = 20;
    /** @hide */
    public static int OP_READ_ICC_SMS = 21;
    /** @hide */
    public static int OP_WRITE_ICC_SMS = 22;
    /** @hide */
    public static int OP_WRITE_SETTINGS = 23;
    /** @hide */
    public static int OP_SYSTEM_ALERT_WINDOW = 24;
    /** @hide */
    public static int OP_ACCESS_NOTIFICATIONS = 25;
    /** @hide */
    public static int OP_CAMERA = 26;
    /** @hide */
    public static int OP_RECORD_AUDIO = 27;
    /** @hide */
    public static int OP_PLAY_AUDIO = 28;
    /** @hide */
    public static int OP_READ_CLIPBOARD = 29;
    /** @hide */
    public static int OP_WRITE_CLIPBOARD = 30;
    /** @hide */
    public static int OP_TAKE_MEDIA_BUTTONS = 31;
    /** @hide */
    public static int OP_TAKE_AUDIO_FOCUS = 32;
    /** @hide */
    public static int OP_AUDIO_MASTER_VOLUME = 33;
    /** @hide */
    public static int OP_AUDIO_VOICE_VOLUME = 34;
    /** @hide */
    public static int OP_AUDIO_RING_VOLUME = 35;
    /** @hide */
    public static int OP_AUDIO_MEDIA_VOLUME = 36;
    /** @hide */
    public static int OP_AUDIO_ALARM_VOLUME = 37;
    /** @hide */
    public static int OP_AUDIO_NOTIFICATION_VOLUME = 38;
    /** @hide */
    public static int OP_AUDIO_BLUETOOTH_VOLUME = 39;
    /** @hide */
    public static int OP_WAKE_LOCK = 40;
    /** @hide Continually monitoring location data. */
    public static int OP_MONITOR_LOCATION = 41;
    /** @hide Continually monitoring location data with a relatively high power request. */
    public static int OP_MONITOR_HIGH_POWER_LOCATION = 42;
    /** @hide Retrieve current usage stats via {@link UsageStatsManager}. */
    public static int OP_GET_USAGE_STATS = 43;
    /** @hide */
    public static int OP_MUTE_MICROPHONE = 44;
    /** @hide */
    public static int OP_TOAST_WINDOW = 45;
    /** @hide Capture the device's display contents and/or audio */
    public static int OP_PROJECT_MEDIA = 46;
    /** @hide Activate a VPN connection without user intervention. */
    public static int OP_ACTIVATE_VPN = 47;
    /** @hide Access the WallpaperManagerAPI to write wallpapers. */
    // since api-23
    public static int OP_WRITE_WALLPAPER = 48;
    /** @hide Received the assist structure from an app. */
    // since api-23
    public static int OP_ASSIST_STRUCTURE = 49;
    /** @hide Received a screenshot from assist. */
    // since api-23
    public static int OP_ASSIST_SCREENSHOT = 50;
    /** @hide Read the phone state. */
    // since api-23
    public static int OP_READ_PHONE_STATE = 51;
    /** @hide Add voicemail messages to the voicemail content provider. */
    // since api-23
    public static int OP_ADD_VOICEMAIL = 52;
    /** @hide Access APIs for SIP calling over VOIP or WiFi. */
    // since api-23
    public static int OP_USE_SIP = 53;
    /** @hide Intercept outgoing calls. */
    // since api-23
    public static int OP_PROCESS_OUTGOING_CALLS = 54;
    /** @hide User the fingerprint API. */
    // since api-23
    public static int OP_USE_FINGERPRINT = 55;
    /** @hide Access to body sensors such as heart rate, etc. */
    // since api-23
    public static int OP_BODY_SENSORS = 56;
    /** @hide Read previously received cell broadcast messages. */
    public static int OP_READ_CELL_BROADCASTS = 57;
    // since api-23
    /** @hide Inject mock location into the system. */
    public static int OP_MOCK_LOCATION = 58;
    // since api-23
    /** @hide Read external storage. */
    public static int OP_READ_EXTERNAL_STORAGE = 59;
    // since api-23
    /** @hide Write external storage. */
    public static int OP_WRITE_EXTERNAL_STORAGE = 60;
    /** @hide Turned on the screen. */
    // since api-23
    public static int OP_TURN_SCREEN_ON = 61;
    /** @hide Get device accounts. */
    // since api-23
    public static int OP_GET_ACCOUNTS = 62;
    /** @hide Control whether an application is allowed to run in the background. */
    // since api-24
    public static int OP_RUN_IN_BACKGROUND = 63;

    /**
     * Retrieve a non-localized name for the operation, for debugging output.
     * @hide
     */
    public static String opToName(int op) {
        throw new UnsupportedOperationException();
    }

    /**
     * Class holding all of the operation information associated with an app.
     * @hide
     */
    public static class PackageOps {

        public String getPackageName() {
            throw new UnsupportedOperationException();
        }

        public int getUid() {
            throw new UnsupportedOperationException();
        }

        public List<OpEntry> getOps() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Class holding the information about one unique operation of an application.
     * @hide
     */
    public static class OpEntry {

        public int getOp() {
            throw new UnsupportedOperationException();
        }

        public int getMode() {
            throw new UnsupportedOperationException();
        }

        public long getTime() {
            throw new UnsupportedOperationException();
        }

        public long getRejectTime() {
            throw new UnsupportedOperationException();
        }

        public boolean isRunning() {
            throw new UnsupportedOperationException();
        }

        public int getDuration() {
            throw new UnsupportedOperationException();
        }

        public int getProxyUid() {
            throw new UnsupportedOperationException();
        }

        public String getProxyPackageName() {
            throw new UnsupportedOperationException();
        }

    }
}