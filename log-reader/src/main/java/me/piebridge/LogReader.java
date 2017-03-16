package me.piebridge;

import java.io.File;

/**
 * LogReader, similar like logcat
 * Created by thom on 2017/1/22.
 */
public class LogReader {

    static {
        String libReader = System.getProperty("java.libreader.path");
        if (libReader != null && new File(libReader).isFile()) {
            System.load(libReader);
        } else {
            System.loadLibrary("reader");
        }
    }

    private LogReader() {

    }

    /**
     * read events since now, would block
     *
     * @param pid     filter event for pid
     * @param handler event handler
     */
    @SuppressWarnings("JniMissingFunction")
    public static native void readEvents(int pid, EventHandler handler);

}
