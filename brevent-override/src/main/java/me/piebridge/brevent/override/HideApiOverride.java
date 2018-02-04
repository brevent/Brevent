package me.piebridge.brevent.override;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.hardware.usb.UsbManager;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.util.List;

/**
 * Created by thom on 2017/2/22.
 */
public class HideApiOverride {

    private static final String TAG = "BreventServer";

    /**
     * deprecated in n
     */
    public static final int USER_OWNER = getUserOwner();

    public static final int _NUM_OP = getNumOp();
    public static final int OP_NONE = getOpNone();
    public static final int OP_GET_USAGE_STATS = getOpGetUsageStats();
    public static final int OP_ACTIVATE_VPN = getOpActivateVpn();
    public static final int OP_RUN_IN_BACKGROUND = getOpRunInBackground();

    public static final String ACTION_USB_STATE = getActionUsbState();
    public static final String USB_CONNECTED = getUsbConnected();
    public static final String USB_DATA_UNLOCKED = getUsbDataUnlocked();

    public static final String SMS_DEFAULT_APPLICATION = getSmsDefaultApplication();

    public static final int ROOT_UID = getRootUid();
    public static final int SHELL_UID = getShellUid();

    private static final int FLAG_PERSISTENT = getFlagPersistent();
    private static final int PROCESS_STATE_CACHED_ACTIVITY = getProcessStateCachedActivity();
    private static int processStateBoundForegroundService;
    private static int processStateForegroundService;
    private static final int PROCESS_STATE_SERVICE = getProcessStateService();
    private static final int PROCESS_STATE_RECEIVER = getProcessStateReceiver();
    private static final int PROCESS_STATE_TOP = getProcessStateTop();

    private static final int RECENT_IGNORE_HOME_STACK_TASKS = getRecentIgnoreHomeStackTasks();
    private static final int RECENT_INCLUDE_PROFILES = getRecentIncludeProfiles();

    private static int flagForwardLock;

    private HideApiOverride() {

    }

    public static int uidForData(int uid) {
        if (uid == ROOT_UID) {
            return SHELL_UID;
        } else {
            return uid;
        }
    }

    public static int getUserId() {
        return UserHandle.myUserId();
    }

    public static int getUserId(int uid) {
        return UserHandle.getUserId(uid);
    }

    public static int myPpid() {
        return Process.myPpid();
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

    public static boolean isSafe(int processState) {
        return processState > PROCESS_STATE_RECEIVER;
    }

    public static boolean isService(int processState) {
        return processState == getProcessStateBoundForegroundService()
                || processState == getProcessStateForegroundService()
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
                getRecentIngoreDockedStackTopTask() |
                getRecentIngorePinnedStackTasks();
    }

    public static int getRecentFlagsM() {
        return RECENT_IGNORE_HOME_STACK_TASKS |
                ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                RECENT_INCLUDE_PROFILES;
    }

    public static boolean isForegroundService(int processState) {
        return processState == getProcessStateForegroundService();
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

    public static int getOpEntryOp(Object opEntry) {
        return ((AppOpsManager.OpEntry) opEntry).getOp();
    }

    public static int getOpEntryMode(Object opEntry) {
        return ((AppOpsManager.OpEntry) opEntry).getMode();
    }

    public static long getOpEntryTime(Object opEntry) {
        return ((AppOpsManager.OpEntry) opEntry).getTime();
    }

    public static long getOpEntryRejectTime(Object opEntry) {
        return ((AppOpsManager.OpEntry) opEntry).getRejectTime();
    }

    public static int opToSwitch(int op) {
        return AppOpsManager.opToSwitch(op);
    }

    public static String opToName(int op) {
        return AppOpsManager.opToName(op);
    }

    public static String opToPermission(int op) {
        return AppOpsManager.opToPermission(op);
    }

    private static int getUserOwner() {
        try {
            return UserHandle.USER_OWNER;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find UserHandle.USER_OWNER");
            return 0;
        }
    }

    private static int getNumOp() {
        try {
            return AppOpsManager._NUM_OP;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find AppOpsManager._NUM_OP");
            return 70;
        }
    }

    private static int getOpNone() {
        try {
            return AppOpsManager.OP_NONE;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find AppOpsManager.OP_NONE");
            return -1;
        }
    }

    private static int getOpActivateVpn() {
        try {
            return AppOpsManager.OP_ACTIVATE_VPN;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find AppOpsManager.OP_ACTIVATE_VPN");
            return 47;
        }
    }

    private static int getOpGetUsageStats() {
        try {
            return AppOpsManager.OP_GET_USAGE_STATS;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find AppOpsManager.OP_GET_USAGE_STATS");
            return 43;
        }
    }

    private static int getOpRunInBackground() {
        try {
            return AppOpsManager.OP_RUN_IN_BACKGROUND;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find AppOpsManager.OP_RUN_IN_BACKGROUND");
            return 63;
        }
    }

    private static String getActionUsbState() {
        try {
            return UsbManager.ACTION_USB_STATE;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find UsbManager.ACTION_USB_STATE");
            return "android.hardware.usb.action.USB_STATE";
        }
    }

    private static String getUsbConnected() {
        try {
            return UsbManager.USB_CONNECTED;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find UsbManager.USB_CONNECTED");
            return "connected";
        }
    }

    private static String getUsbDataUnlocked() {
        try {
            return UsbManager.USB_DATA_UNLOCKED;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find UsbManager.USB_DATA_UNLOCKED");
            return "unlocked";
        }
    }

    private static String getCallMethodUserKey() {
        try {
            return Settings.CALL_METHOD_USER_KEY;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find Settings.CALL_METHOD_USER_KEY");
            return "_user";
        }
    }

    private static String getCallMethodGetSecure() {
        try {
            return Settings.CALL_METHOD_GET_SECURE;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find Settings.CALL_METHOD_GET_SECURE");
            return "GET_secure";
        }
    }

    private static String getCallMethodGetGlobal() {
        try {
            return Settings.CALL_METHOD_GET_GLOBAL;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find Settings.CALL_METHOD_GET_GLOBAL");
            return "GET_global";
        }
    }

    private static String getSmsDefaultApplication() {
        try {
            return Settings.Secure.SMS_DEFAULT_APPLICATION;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find Settings.Secure.SMS_DEFAULT_APPLICATION");
            return "sms_default_application";
        }
    }

    public static String getDialerDefaultApplication() {
        try {
            return Settings.Secure.DIALER_DEFAULT_APPLICATION;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find Settings.Secure.DIALER_DEFAULT_APPLICATION");
            return "dialer_default_application";
        }
    }

    public static String getAssistant() {
        try {
            return Settings.Secure.ASSISTANT;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find Settings.Secure.ASSISTANT");
            return "assistant";
        }
    }

    public static String getVoiceInteractionService() {
        try {
            return Settings.Secure.VOICE_INTERACTION_SERVICE;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find Settings.Secure.VOICE_INTERACTION_SERVICE");
            return "voice_interaction_service";
        }
    }

    public static String getEnabledNotificationListeners() {
        try {
            return Settings.Secure.ENABLED_NOTIFICATION_LISTENERS;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find Settings.Secure.ENABLED_NOTIFICATION_LISTENERS");
            return "enabled_notification_listeners";
        }
    }

    private static int getRootUid() {
        try {
            return Process.ROOT_UID;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find Process.ROOT_UID");
            return 0;
        }
    }

    private static int getShellUid() {
        try {
            return Process.SHELL_UID;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find Process.SHELL_UID");
            return 2000;
        }
    }

    private static int getFlagPersistent() {
        try {
            return ActivityManager.RunningAppProcessInfo.FLAG_PERSISTENT;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.FLAG_PERSISTENT");
            return 1 << 1;
        }
    }

    private static int getProcessStateCachedActivity() {
        try {
            return ActivityManager.PROCESS_STATE_CACHED_ACTIVITY;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.PROCESS_STATE_CACHED_ACTIVITY");
            return 14;
        }
    }

    private static int getProcessStateBoundForegroundService() {
        if (processStateBoundForegroundService == 0) {
            processStateBoundForegroundService = getProcessStateBoundForegroundServiceM();
        }
        return processStateBoundForegroundService;
    }

    private static int getProcessStateBoundForegroundServiceM() {
        try {
            return ActivityManager.PROCESS_STATE_BOUND_FOREGROUND_SERVICE;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.PROCESS_STATE_BOUND_FOREGROUND_SERVICE");
            return 3;
        }
    }

    private static int getProcessStateForegroundService() {
        if (processStateForegroundService == 0) {
            processStateForegroundService = getProcessStateForegroundServiceM();
        }
        return processStateForegroundService;
    }

    private static int getProcessStateForegroundServiceM() {
        try {
            return ActivityManager.PROCESS_STATE_FOREGROUND_SERVICE;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.PROCESS_STATE_FOREGROUND_SERVICE");
            return 4;
        }
    }

    private static int getProcessStateService() {
        try {
            return ActivityManager.PROCESS_STATE_SERVICE;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.PROCESS_STATE_SERVICE");
            return 10;
        }
    }

    private static int getProcessStateReceiver() {
        try {
            return ActivityManager.PROCESS_STATE_RECEIVER;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.PROCESS_STATE_RECEIVER");
            return 11;
        }
    }

    private static int getProcessStateTop() {
        try {
            return ActivityManager.PROCESS_STATE_TOP;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.PROCESS_STATE_TOP");
            return 2;
        }
    }

    private static int getRecentIgnoreHomeStackTasks() {
        try {
            return ActivityManager.RECENT_IGNORE_HOME_STACK_TASKS;
        } catch (LinkageError e) {
            // do nothing
        }
        try {
            return ActivityManager.RECENT_IGNORE_HOME_AND_RECENTS_STACK_TASKS;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.RECENT_IGNORE_HOME_STACK_TASKS" +
                    " nor ActivityManager.RECENT_IGNORE_HOME_AND_RECENTS_STACK_TASKS");
        }
        return 0x0008;
    }

    private static int getRecentIncludeProfiles() {
        try {
            return ActivityManager.RECENT_INCLUDE_PROFILES;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.RECENT_INCLUDE_PROFILES");
            return 0x0004;
        }
    }

    private static int getRecentIngorePinnedStackTasks() {
        try {
            return ActivityManager.RECENT_INGORE_PINNED_STACK_TASKS;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.RECENT_INGORE_PINNED_STACK_TASKS");
            return 0x0020;
        }
    }

    private static int getRecentIngoreDockedStackTopTask() {
        try {
            return ActivityManager.RECENT_INGORE_DOCKED_STACK_TOP_TASK;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find ActivityManager.RECENT_INGORE_DOCKED_STACK_TOP_TASK");
            return 0x0010;
        }
    }

    public static int getFlagForwardLock() {
        try {
            return ApplicationInfo.FLAG_FORWARD_LOCK;
        } catch (LinkageError e) {
            Log.w(TAG, "Can't find  ApplicationInfo.FLAG_FORWARD_LOCK");
            return 1 << 29;
        }
    }

    public static AssetManager newAssetManager() {
        return new AssetManager();
    }

    public static int addAssetPath(AssetManager assetManager, String path) {
        return assetManager.addAssetPath(path);
    }

    public static long getLaunchCount(UsageStats stats) {
        return stats.mLaunchCount;
    }

    public static long getLastEvent(UsageStats stats) {
        return stats.mLastEvent;
    }

    public static boolean isForwardLockedM(ApplicationInfo applicationInfo) {
        return applicationInfo.isForwardLocked();
    }

    public static boolean isForwardLockedL(int flags) {
        if (flagForwardLock == 0) {
            flagForwardLock = getFlagForwardLock();
        }
       return (flags & flagForwardLock) != 0;
    }

}
