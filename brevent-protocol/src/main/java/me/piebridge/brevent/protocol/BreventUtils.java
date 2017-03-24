package me.piebridge.brevent.protocol;

import android.content.res.Resources;
import android.os.Build;

/**
 * Created by thom on 2017/3/25.
 */
public class BreventUtils {

    private BreventUtils() {

    }

    public static boolean supportStandby() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        Resources system = Resources.getSystem();
        int identifier = system.getIdentifier("config_enableAutoPowerModes", "bool", "android");
        try {
            return identifier != 0 && system.getBoolean(identifier);
        } catch (Resources.NotFoundException e) { // NOSONAR
            return false;
        }
    }

}
