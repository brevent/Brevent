package me.piebridge.brevent.override;

import android.content.pm.ApplicationInfo;

/**
 * Created by thom on 2018/1/18.
 */
public class HideApiOverrideO {

    private HideApiOverrideO() {

    }

    public static boolean isInstantApp(ApplicationInfo applicationInfo) {
        return applicationInfo.isInstantApp();
    }

}
