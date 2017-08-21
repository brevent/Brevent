package android.os;

/**
 * Created by thom on 2017/2/21.
 */
public class Process {

    /**
     * Defines the root UID.
     *
     * @hide
     */
    public static int ROOT_UID = 0;

    /**
     * Defines the UID/GID for the user shell.
     *
     * @hide
     */
    public static int SHELL_UID = 2000;

    /**
     * Returns the identifier of this process' parent.
     *
     * @hide
     */
    public static int myPpid() {
        throw new UnsupportedOperationException();
    }

    /** @hide */
    public static int[] getPidsForCommands(String[] cmds) {
        throw new UnsupportedOperationException();
    }

}
