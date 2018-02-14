package android.os;

/**
 * Created by thom on 2018/2/14.
 */
public class StrictMode {

    /**
     * Used by the framework to make file usage a fatal error.
     *
     * @hide
     */
    public static void enableDeathOnFileUriExposure() {
        throw new UnsupportedOperationException();
    }

    /**
     * Used by lame internal apps that haven't done the hard work to get
     * themselves off file:// Uris yet.
     *
     * @hide
     */
    public static void disableDeathOnFileUriExposure() {
        throw new UnsupportedOperationException();
    }

}
