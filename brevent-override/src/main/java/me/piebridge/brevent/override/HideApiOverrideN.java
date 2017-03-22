package me.piebridge.brevent.override;

import android.app.ActivityManager;
import android.os.UserHandle;
import android.provider.Settings;

/**
 * hide api for n
 * Created by thom on 2017/2/22.
 */
public class HideApiOverrideN {

    public static final int USER_SYSTEM = getUserSystem();

    public static final String WEBVIEW_PROVIDER = getWebviewProvider();
    public static final String QS_TILES = getQsTiles();

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
            return 0;
        }
    }

    private static String getWebviewProvider() {
        try {
            return Settings.Global.WEBVIEW_PROVIDER;
        } catch (LinkageError e) {
            return "webview_provider";
        }
    }

    private static int getFreeformWorkspaceStackId() {
        try {
            return ActivityManager.StackId.FREEFORM_WORKSPACE_STACK_ID;
        } catch (LinkageError e) {
            return 2;
        }
    }

    private static String getQsTiles() {
        try {
            return Settings.Secure.QS_TILES;
        } catch (LinkageError e) {
            return "sysui_qs_tiles";
        }
    }

}
