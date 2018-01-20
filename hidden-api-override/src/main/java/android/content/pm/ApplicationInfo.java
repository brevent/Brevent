package android.content.pm;

/**
 * Created by thom on 2018/1/18.
 */
public class ApplicationInfo {

    public static int FLAG_FORWARD_LOCK = 1 << 29;

    /**
     * @hide
     */
    public boolean isInstantApp() {
        throw new UnsupportedOperationException();
    }

    /** @hide */
    public boolean isForwardLocked() {
        throw new UnsupportedOperationException();
    }

    /** @hide */
    public boolean isInternal() {
        throw new UnsupportedOperationException();
    }

}
