package me.piebridge.brevent.override;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;

import java.util.List;

/**
 * Created by thom on 2017/2/22.
 */
public class HideApiOverride {

    /**
     * deprecated in n
     */
    public static final int USER_OWNER = getUserOwner();

    public static final int OP_NONE = getOpNone();
    public static final int OP_ACTIVATE_VPN = getOpActivateVpn();

    public static final String ACTION_USB_STATE = getActionUsbState();
    public static final String USB_CONNECTED = getUsbConnected();

    public static final String CALL_METHOD_USER_KEY = getCallMethodUserKey();
    public static final String CALL_METHOD_GET_SECURE = getCallMethodGetSecure();
    public static final String CALL_METHOD_GET_GLOBAL = getCallMethodGetGlobal();

    public static final String SMS_DEFAULT_APPLICATION = getSmsDefaultApplication();
    public static final String DIALER_DEFAULT_APPLICATION = getDialerDefaultApplication();
    public static final String ASSISTANT = getAssistant();

    private static final int ROOT_UID = getRootUid();
    private static final int SHELL_UID = getShellUid();

    private static final int FLAG_PERSISTENT = getFlagPersistent();
    private static final int PROCESS_STATE_CACHED_ACTIVITY = getProcessStateCachedActivity();
    private static final int PROCESS_STATE_BOUND_FOREGROUND_SERVICE = getProcessStateBoundForegroundService();
    private static final int PROCESS_STATE_FOREGROUND_SERVICE = getProcessStateForegroundService();
    private static final int PROCESS_STATE_SERVICE = getProcessStateService();
    private static final int PROCESS_STATE_RECEIVER = getProcessStateReceiver();
    private static final int PROCESS_STATE_TOP = getProcessStateTop();

    private static final int RECENT_IGNORE_HOME_STACK_TASKS = getRecentIgnoreHomeStackTasks();
    private static final int RECENT_INCLUDE_PROFILES = getRecentIncludeProfiles();
    private static final int RECENT_INGORE_DOCKED_STACK_TOP_TASK = getRecentIngoreDockedStackTopTask();
    private static final int RECENT_INGORE_PINNED_STACK_TASKS = getRecentIngorePinnedStackTasks();

    private HideApiOverride() {

    }

    public static int uidForData(int uid) {
        if (uid == ROOT_UID) {
            return SHELL_UID;
        } else {
            return uid;
        }
    }

    public static boolean isRoot(int uid) {
        return uid == ROOT_UID;
    }

    public static int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }


    public static int getProcessState(ActivityManager.RunningAppProcessInfo process) {
        return process.processState;
    }

    public static boolean isPersistent(ActivityManager.RunningAppProcessInfo process) {
        return (process.flags & FLAG_PERSISTENT) == FLAG_PERSISTENT;
    }

    public static boolean isCached(int processState) {
        return processState >= PROCESS_STATE_CACHED_ACTIVITY;
    }

    public static boolean isService(int processState) {
        return processState == PROCESS_STATE_BOUND_FOREGROUND_SERVICE
                || processState == PROCESS_STATE_FOREGROUND_SERVICE
                || processState == PROCESS_STATE_SERVICE
                || processState == PROCESS_STATE_RECEIVER;
    }

    public static boolean isServiceL(int processState) {
        return processState == PROCESS_STATE_SERVICE
                || processState == PROCESS_STATE_RECEIVER;
    }

    public static boolean isTop(int processState) {
        return processState == PROCESS_STATE_TOP;
    }

    public static long getLastActiveTime(ActivityManager.RecentTaskInfo taskInfo) {
        return taskInfo.lastActiveTime;
    }

    public static int getRecentFlags() {
        return RECENT_IGNORE_HOME_STACK_TASKS |
                ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                RECENT_INCLUDE_PROFILES |
                RECENT_INGORE_DOCKED_STACK_TOP_TASK |
                RECENT_INGORE_PINNED_STACK_TASKS;
    }

    public static int getRecentFlagsM() {
        return RECENT_IGNORE_HOME_STACK_TASKS |
                ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                RECENT_INCLUDE_PROFILES;
    }

    public static boolean isForegroundService(int processState) {
        return processState == PROCESS_STATE_FOREGROUND_SERVICE;
    }

    public static String resolveCallingPackage(int uid) {
        if (uid == ROOT_UID) {
            return "root";
        } else if (uid == SHELL_UID) {
            return "com.android.shell";
        } else {
            return null;
        }
    }

    public static String getPairValue(Bundle result) {
        return result.getPairValue();
    }

    public static int getPackageOpsUid(Object packageOps) {
        return ((AppOpsManager.PackageOps) packageOps).getUid();
    }

    public static List getPackageOpsOps(Object packageOps) {
        return ((AppOpsManager.PackageOps) packageOps).getOps();
    }

    public static String getPackageOpsPackageName(Object packageOps) {
        return ((AppOpsManager.PackageOps) packageOps).getPackageName();
    }

    public static int getOpEntryMode(Object opEntry) {
        return ((AppOpsManager.OpEntry) opEntry).getMode();
    }

    public static long getOpEntryTime(Object opEntry) {
        return ((AppOpsManager.OpEntry) opEntry).getTime();
    }

    private static int getUserOwner() {
        try {
            return UserHandle.USER_OWNER;
        } catch (LinkageError e) {
            return 0;
        }
    }

    private static int getOpNone() {
        try {
            return AppOpsManager.OP_NONE;
        } catch (LinkageError e) {
            return -1;
        }
    }

    private static int getOpActivateVpn() {
        try {
            return AppOpsManager.OP_ACTIVATE_VPN;
        } catch (LinkageError e) {
            return 47;
        }
    }

    private static String getActionUsbState() {
        try {
            return UsbManager.ACTION_USB_STATE;
        } catch (LinkageError e) {
            return "android.hardware.usb.action.USB_STATE";
        }
    }

    private static String getUsbConnected() {
        try {
            return UsbManager.USB_CONNECTED;
        } catch (LinkageError e) {
            return "connected";
        }
    }

    private static String getCallMethodUserKey() {
        try {
            return Settings.CALL_METHOD_USER_KEY;
        } catch (LinkageError e) {
            return "_user";
        }
    }

    private static String getCallMethodGetSecure() {
        try {
            return Settings.CALL_METHOD_GET_SECURE;
        } catch (LinkageError e) {
            return "GET_secure";
        }
    }

    private static String getCallMethodGetGlobal() {
        try {
            return Settings.CALL_METHOD_GET_GLOBAL;
        } catch (LinkageError e) {
            return "GET_global";
        }
    }

    private static String getSmsDefaultApplication() {
        try {
            return Settings.Secure.SMS_DEFAULT_APPLICATION;
        } catch (LinkageError e) {
            return "sms_default_application";
        }
    }

    public static String getDialerDefaultApplication() {
        try {
            return Settings.Secure.DIALER_DEFAULT_APPLICATION;
        } catch (LinkageError e) {
            return "dialer_default_application";
        }
    }

    public static String getAssistant() {
        try {
            return Settings.Secure.ASSISTANT;
        } catch (LinkageError e) {
            return "assistant";
        }
    }

    private static int getRootUid() {
        try {
            return Process.ROOT_UID;
        } catch (LinkageError e) {
            return 0;
        }
    }

    private static int getShellUid() {
        try {
            return Process.SHELL_UID;
        } catch (LinkageError e) {
            return 2000;
        }
    }

    private static int getFlagPersistent() {
        try {
            return ActivityManager.RunningAppProcessInfo.FLAG_PERSISTENT;
        } catch (LinkageError e) {
            return 1 << 1;
        }
    }

    private static int getProcessStateCachedActivity() {
        try {
            return ActivityManager.PROCESS_STATE_CACHED_ACTIVITY;
        } catch (LinkageError e) {
            return 14;
        }
    }

    private static int getProcessStateBoundForegroundService() {
        try {
            return ActivityManager.PROCESS_STATE_BOUND_FOREGROUND_SERVICE;
        } catch (LinkageError e) {
            return 3;
        }
    }

    private static int getProcessStateForegroundService() {
        try {
            return ActivityManager.PROCESS_STATE_FOREGROUND_SERVICE;
        } catch (LinkageError e) {
            return 4;
        }
    }

    private static int getProcessStateService() {
        try {
            return ActivityManager.PROCESS_STATE_SERVICE;
        } catch (LinkageError e) {
            return 10;
        }
    }

    private static int getProcessStateReceiver() {
        try {
            return ActivityManager.PROCESS_STATE_RECEIVER;
        } catch (LinkageError e) {
            return 11;
        }
    }

    private static int getProcessStateTop() {
        try {
            return ActivityManager.PROCESS_STATE_TOP;
        } catch (LinkageError e) {
            return 2;
        }
    }

    private static int getRecentIgnoreHomeStackTasks() {
        try {
            return ActivityManager.RECENT_IGNORE_HOME_STACK_TASKS;
        } catch (LinkageError e) {
            return 0x0008;
        }
    }

    private static int getRecentIncludeProfiles() {
        try {
            return ActivityManager.RECENT_INCLUDE_PROFILES;
        } catch (LinkageError e) {
            return 0x0004;
        }
    }

    private static int getRecentIngorePinnedStackTasks() {
        try {
            return ActivityManager.RECENT_INGORE_PINNED_STACK_TASKS;
        } catch (LinkageError e) {
            return 0x0020;
        }
    }

    private static int getRecentIngoreDockedStackTopTask() {
        try {
            return ActivityManager.RECENT_INGORE_DOCKED_STACK_TOP_TASK;
        } catch (LinkageError e) {
            return 0x0010;
        }
    }

}
