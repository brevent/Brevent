package me.piebridge.brevent.protocol;

import android.os.SystemClock;

import java.util.concurrent.TimeUnit;

/**
 * Created by thom on 2016/10/20.
 */
public class TimeUtils {

    private TimeUtils() {

    }

    public static int now() {
        // i don't think can larger than Integer.MAX_VALUE, which is boot since 68 years
        return (int) TimeUnit.MILLISECONDS.toSeconds(SystemClock.elapsedRealtime());
    }

}
