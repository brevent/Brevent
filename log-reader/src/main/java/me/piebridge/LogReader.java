package me.piebridge;

import android.text.TextUtils;
import android.util.Log;

/**
 * LogReader, similar like logcat
 * Created by thom on 2017/1/22.
 */
public class LogReader {

    static {
        String libReader = System.getProperty("java.libreader.path");
        Log.d("BreventLoader", "libReader: " + libReader);
        if (TextUtils.isEmpty(libReader)) {
            System.loadLibrary("reader");
        } else {
            System.loadLibrary(libReader);
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
