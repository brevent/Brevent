package me.piebridge.brevent.override;

import android.app.ActivityManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

/**
 * hide api for n
 * Created by thom on 2017/2/22.
 */
public class HideApiOverrideN {

    private static final String TAG = "BreventServer";

    public static final int USER_SYSTEM = getUserSystem();

    public static final String WEBVIEW_PROVIDER = getWebviewProvider();

    public static final int FREEFORM_WORKSPACE_STACK_ID = getFreeformWorkspaceStackId();

    private HideApiOverrideN() {

    }

    public static boolean isFreeForm(ActivityManager.RecentTaskInfo taskInfo) {
        return taskInfo.stackId == FREEFORM_WORKSPACE_STACK_ID;
    }

    private static int getUserSystem() {
        try {
            return UserHandle.USER_SYSTEM;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find UserHandle.USER_SYSTEM");
            return 0;
        }
    }

    private static String getWebviewProvider() {
        try {
            return Settings.Global.WEBVIEW_PROVIDER;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find Settings.Global.WEBVIEW_PROVIDER");
            return "webview_provider";
        }
    }

    private static int getFreeformWorkspaceStackId() {
        try {
            return ActivityManager.StackId.FREEFORM_WORKSPACE_STACK_ID;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.StackId.FREEFORM_WORKSPACE_STACK_ID");
            return 2;
        }
    }

}
