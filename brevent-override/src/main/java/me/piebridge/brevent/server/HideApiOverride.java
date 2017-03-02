package me.piebridge.brevent.server;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.os.Process;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by thom on 2017/2/22.
 */
public class HideApiOverride {

    /**
     * deprecated in n
     */
    public static final int USER_OWNER = UserHandle.USER_OWNER;

    public static final int OP_NONE = AppOpsManager.OP_NONE;

    public static final int OP_POST_NOTIFICATION = AppOpsManager.OP_POST_NOTIFICATION;

    private HideApiOverride() {

    }

    public static int uidForData(int uid) {
        if (uid == Process.ROOT_UID) {
            return Process.SHELL_UID;
        } else {
            return uid;
        }
    }

    public static boolean isRoot(int uid) {
        return uid == Process.ROOT_UID;
    }

    public static Collection<String> filterOps(List ops, int mode, int uid) {
        Collection<String> packageNames = new ArrayList<>();
        for (Object op : ops) {
            AppOpsManager.PackageOps pkg = (AppOpsManager.PackageOps) op;
            if (pkg.getUid() == uid) {
                for (AppOpsManager.OpEntry entry : pkg.getOps()) {
                    if (entry.getMode() == mode) {
                        packageNames.add(pkg.getPackageName());
                        break;
                    }
                }
            }
        }
        return packageNames;
    }

    public static String opToName(int op) {
        return AppOpsManager.opToName(op);
    }

    public static int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }


    public static int getProcessState(ActivityManager.RunningAppProcessInfo process) {
        return process.processState;
    }

    public static boolean isPersistent(ActivityManager.RunningAppProcessInfo process) {
        return (process.flags & ActivityManager.RunningAppProcessInfo.FLAG_PERSISTENT) == ActivityManager.RunningAppProcessInfo.FLAG_PERSISTENT;
    }

    public static boolean isCached(int processState) {
        return processState >= ActivityManager.PROCESS_STATE_CACHED_ACTIVITY;
    }

    public static boolean isService(int processState) {
        return processState == ActivityManager.PROCESS_STATE_BOUND_FOREGROUND_SERVICE
                || processState == ActivityManager.PROCESS_STATE_FOREGROUND_SERVICE
                || processState == ActivityManager.PROCESS_STATE_SERVICE
                || processState == ActivityManager.PROCESS_STATE_RECEIVER;
    }

    public static boolean isServiceL(int processState) {
        return processState == ActivityManager.PROCESS_STATE_SERVICE
                || processState == ActivityManager.PROCESS_STATE_RECEIVER;
    }

    public static boolean isTop(int processState) {
        return processState == ActivityManager.PROCESS_STATE_TOP;
    }

    public static long getLastActiveTime(ActivityManager.RecentTaskInfo taskInfo) {
        return taskInfo.lastActiveTime;
    }


    public static int getRecentFlags() {
        return ActivityManager.RECENT_IGNORE_HOME_STACK_TASKS |
                ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                ActivityManager.RECENT_INCLUDE_PROFILES |
                ActivityManager.RECENT_INGORE_DOCKED_STACK_TOP_TASK |
                ActivityManager.RECENT_INGORE_PINNED_STACK_TASKS;
    }

    public static int getRecentFlagsM() {
        return ActivityManager.RECENT_IGNORE_HOME_STACK_TASKS |
                ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                ActivityManager.RECENT_INCLUDE_PROFILES;
    }

}
