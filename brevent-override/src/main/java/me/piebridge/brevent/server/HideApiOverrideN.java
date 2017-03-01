package me.piebridge.brevent.server;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.os.UserHandle;

/**
 * hide api for n
 * Created by thom on 2017/2/22.
 */
public class HideApiOverrideN {

    public static final int USER_SYSTEM = UserHandle.USER_SYSTEM;

    public static final int OP_RUN_IN_BACKGROUND = AppOpsManager.OP_RUN_IN_BACKGROUND;

    private HideApiOverrideN() {

    }

    public static boolean isFreeForm(ActivityManager.RecentTaskInfo taskInfo) {
        return taskInfo.stackId == ActivityManager.StackId.FREEFORM_WORKSPACE_STACK_ID;
    }

}
