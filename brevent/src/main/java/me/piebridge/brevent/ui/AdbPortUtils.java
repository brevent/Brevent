package me.piebridge.brevent.ui;

import android.os.SystemProperties;
import android.text.TextUtils;

/**
 * Created by thom on 2017/11/15.
 */
public class AdbPortUtils {

    private AdbPortUtils() {

    }

    public static int getAdbPort() {
        // XXX: SystemProperties.get is @hide method
        String port = SystemProperties.get("service.adb.tcp.port", "");
        UILog.d("service.adb.tcp.port: " + port);
        if (!TextUtils.isEmpty(port) && TextUtils.isDigitsOnly(port)) {
            int p = Integer.parseInt(port);
            if (p > 0 && p <= 0xffff) {
                return p;
            }
        }
        return -1;
    }

}
