package me.piebridge;

/**
 * LogReader, similar like logcat
 * Created by thom on 2017/1/22.
 */
public class LogReader {

    static {
        System.loadLibrary("reader");
    }

    private LogReader() {

    }

    /**
     * read events since now, would block
     *
     * @param pid     filter event for pid
     * @param since   event since
     * @param handler event handler
     */
    @SuppressWarnings("JniMissingFunction")
    public static native void readEvents(int pid, long since, EventHandler handler);

}
