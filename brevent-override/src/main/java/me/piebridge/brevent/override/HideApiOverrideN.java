package me.piebridge.brevent.override;

import android.app.ActivityManager;
import android.os.UserHandle;
import android.provider.Settings;

/**
 * hide api for n
 * Created by thom on 2017/2/22.
 */
public class HideApiOverrideN {

    public static final int USER_SYSTEM = UserHandle.USER_SYSTEM;

    public static final String WEBVIEW_PROVIDER = Settings.Global.WEBVIEW_PROVIDER;

    private HideApiOverrideN() {

    }

    public static boolean isFreeForm(ActivityManager.RecentTaskInfo taskInfo) {
        return taskInfo.stackId == ActivityManager.StackId.FREEFORM_WORKSPACE_STACK_ID;
    }

    public static String getQsTiles() {
        return Settings.Secure.QS_TILES;
    }

}
