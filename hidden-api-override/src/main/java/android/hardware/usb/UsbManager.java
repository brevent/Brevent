package android.hardware.usb;

/**
 * Created by thom on 2017/3/10.
 */
public class UsbManager {

    public static String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";

    /**
     * Boolean extra indicating whether USB is connected or disconnected.
     * Used in extras for the {@link #ACTION_USB_STATE} broadcast.
     * <p>
     * {@hide}
     */
    public static String USB_CONNECTED = "connected";

    /**
     * Boolean extra indicating whether confidential user data, such as photos, should be
     * made available on the USB connection. This variable will only be set when the user
     * has explicitly asked for this data to be unlocked.
     * Used in extras for the {@link #ACTION_USB_STATE} broadcast.
     *
     * {@hide}
     */
    public static String USB_DATA_UNLOCKED = "unlocked";

}
