package android.provider;

/**
 * Created by thom on 2017/3/18.
 */
public class Settings {

    /**
     * @hide - User handle argument extra to the fast-path call()-based requests
     */
    public static String CALL_METHOD_USER_KEY = "_user";

    /**
     * @hide - Private call() method on SettingsProvider to read from 'secure' table.
     */
    public static String CALL_METHOD_GET_SECURE = "GET_secure";

    /**
     * @hide - Private call() method on SettingsProvider to read from 'global' table.
     */
    public static String CALL_METHOD_GET_GLOBAL = "GET_global";

    public static class Secure {

        /**
         * Specifies the package name currently configured to be the primary sms application
         *
         * @hide
         */
        public static String SMS_DEFAULT_APPLICATION = "sms_default_application";

        /**
         * Specifies the package name currently configured to be the default dialer application
         *
         * @hide
         */
        public static String DIALER_DEFAULT_APPLICATION = "dialer_default_application";

        /**
         * The current assistant component. It could be a voice interaction service,
         * or an activity that handles ACTION_ASSIST, or empty which means using the default
         * handling.
         *
         * @hide
         */
        public static String ASSISTANT = "assistant";

        /**
         * The currently selected voice interaction service flattened ComponentName.
         *
         * @hide
         */
        public static String VOICE_INTERACTION_SERVICE = "voice_interaction_service";

        /**
         * Names of the service components that the current user has explicitly allowed to
         * see all of the user's notifications, separated by ':'.
         *
         * @hide
         */
        public static String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    }

    public static class Global {

        /**
         * Name of the package used as WebView provider (if unset the provider is instead determined
         * by the system).
         *
         * @hide
         */
        public static String WEBVIEW_PROVIDER = "webview_provider";

    }

}