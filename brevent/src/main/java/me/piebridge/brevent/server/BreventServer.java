package me.piebridge.brevent.server;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.support.v4.util.ArraySet;
import android.support.v4.util.SimpleArrayMap;
import android.text.format.DateUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.SparseIntArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventPackages;
import me.piebridge.brevent.protocol.BreventProtocol;
import me.piebridge.brevent.protocol.BreventStatus;
import me.piebridge.brevent.protocol.BreventToken;
import me.piebridge.brevent.protocol.TimeUtils;

/**
 * Brevent Server
 * Created by thom on 2017/2/13.
 */
public class BreventServer extends Handler {

    private static final String MOVE_TASK_TO_BACK = "moveTaskToBack";

    private static final String OPEN_LAUNCHER = "openLauncher";

    static final int MESSAGE_EVENT = 1;
    static final int MESSAGE_REQUEST_STATUS = 2;
    static final int MESSAGE_REQUEST_MANAGE = 3;

    private static final int MESSAGE_SAVE_BREVENT_LIST = 4;
    private static final int MESSAGE_SAVE_BREVENT_CONF = 5;

    private static final int MESSAGE_CHECK = 6;
    private static final int MESSAGE_CHECK_CHANGED = 7;

    private static final int MESSAGE_EXIT = 8;
    static final int MESSAGE_DEAD = 9;
    private static final int MESSAGE_UPDATE = 10;

    private static final int MAX_TIMEOUT = 30;

    private volatile boolean screen = true;

    private final String mDataDir;

    private UUID mToken;

    private final String mVersionName;

    private final Set<String> mInstalled = new ArraySet<>();
    private final Set<String> mGcm = new ArraySet<>();
    private final Set<String> mBrevent = new ArraySet<>();
    private final BreventConfiguration mConfiguration = new BreventConfiguration(null);

    /**
     * may not need
     */
    private final Set<String> mServices;

    private final Set<String> mBack;

    private final Set<String> mChanged;

    private int repeatCount;
    private int mHomeTid;
    private int homeTid;
    private int possibleHomeTid;
    private String mFocusReason;
    private String mLauncher;

    private int mUser;

    private volatile String mPackageName;

    private static final int CHECK_LATER_USER = 3000;
    private static final int CHECK_LATER_BACK = 3000;
    private static final int CHECK_LATER_HOME = 3000;
    private static final int CHECK_LATER_ANSWER = 3000;
    private static final int CHECK_LATER_SERVICE = 30000;
    private static final int CHECK_LATER_APPS = 60000;
    private static final int CHECK_LATER_SCREEN_OFF = 60000;
    private static final int CHECK_LATER_RECENT = 1800000;
    private static final int CHECK_LATER_UPDATE = 60000;

    private static final int RECENT_FLAGS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
            HideApiOverride.getRecentFlags() : HideApiOverride.getRecentFlagsM();

    private static final int HOME_INTENT_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;

    private static final int NAX_SURVIVE_TIME = 1000 * 60 * 60 * 3;

    private final long mTime;

    private volatile boolean hasNotificationCancelAll = false;

    private BreventServer() throws IOException {
        super();
        mTime = System.currentTimeMillis();
        PackageInfo packageInfo = HideApi.getPackageInfo(BuildConfig.APPLICATION_ID, 0, HideApi.USER_OWNER);
        mVersionName = packageInfo.versionName;
        if (!BuildConfig.VERSION_NAME.equals(mVersionName)) {
            ServerLog.w("version unmatched, running: " + BuildConfig.VERSION_NAME + ", installed: " + mVersionName);
        }

        mUser = HideApi.getCurrentUser();
        ServerLog.i("brevent user: " + mUser);

        mDataDir = getDataDir(HideApi.USER_OWNER);
        if (mDataDir == null) {
            throw new FileNotFoundException("cannot find brevent data directory");
        }

        ServerLog.i("brevent data directory: " + mDataDir);
        loadBreventList();
        loadBreventConf();

        mServices = new ArraySet<>();
        mChanged = new ArraySet<>();
        mBack = new ArraySet<>();

        mLauncher = HideApi.getLauncher(mUser);

        if (!mConfiguration.allowRoot && HideApiOverride.isRoot(Process.myUid())) {
            sendEmptyMessageDelayed(MESSAGE_EXIT, CHECK_LATER_USER);
        }

        handleStatus(BreventToken.EMPTY_TOKEN);
    }

    private void checkAlive() {
        sendEmptyMessageDelayed(MESSAGE_DEAD, CHECK_LATER_ANSWER);
        EventLog.writeEvent(EventTag.TAG_ANSWER, 0xfee1900d);
    }

    private boolean loadBreventConf() {
        File file = getBreventConf();
        ServerLog.i("loading brevent conf");
        if (file.isFile()) {
            try (
                    BufferedReader reader = new BufferedReader(new FileReader(file))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int index = line.indexOf('=');
                    if (index > 0) {
                        String key = line.substring(0, index);
                        String value = line.substring(index + 1);
                        mConfiguration.setValue(key, value);
                    }
                }
            } catch (IOException e) {
                ServerLog.w("Can't load configuration from " + file, e);
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean loadBreventList() {
        for (PackageInfo packageInfo : HideApi.getInstalledPackages(HideApi.USER_OWNER)) {
            mInstalled.add(packageInfo.packageName);
        }
        for (PackageInfo packageInfo : HideApi.getGcmPackages(HideApi.USER_OWNER)) {
            if (HideApi.hasGcmReceiver(packageInfo.packageName, HideApi.USER_OWNER)) {
                mGcm.add(packageInfo.packageName);
            }
        }
        File file = getBreventList();
        ServerLog.i("loading brevent list");
        if (file.isFile()) {
            try (
                    BufferedReader reader = new BufferedReader(new FileReader(file))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (mInstalled.contains(line)) {
                        mBrevent.add(line);
                    }
                }
                ServerLog.i("load " + file.getAbsolutePath() + ", size: " + mBrevent.size());
            } catch (IOException e) {
                ServerLog.w("Can't load brevent from " + file, e);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case MESSAGE_EVENT:
                @SuppressWarnings("unchecked")
                SimpleArrayMap<String, Object> data = (SimpleArrayMap<String, Object>) message.obj;
                handleEvent(message.arg1, message.arg2, data);
                break;
            case MESSAGE_REQUEST_STATUS:
            case MESSAGE_REQUEST_MANAGE:
                if (mUser == HideApi.USER_OWNER) {
                    handleRequest((BreventProtocol) message.obj);
                }
                break;
            case MESSAGE_SAVE_BREVENT_LIST:
                saveBreventList();
                break;
            case MESSAGE_SAVE_BREVENT_CONF:
                saveBreventConf();
                break;
            case MESSAGE_CHECK_CHANGED:
                checkChanged();
                break;
            case MESSAGE_CHECK:
                checkAndBrevent();
                break;
            case MESSAGE_EXIT:
                ServerLog.e("Can't be run as root, please open Brevent");
                System.exit(0);
                break;
            case MESSAGE_DEAD:
                ServerLog.e("Don't receive answer from universe");
                System.exit(1);
                break;
            case MESSAGE_UPDATE:
                quitSafely();
                break;
            default:
                break;
        }
    }

    private void quitSafely() {
        //noinspection ConstantConditions
        Looper.myLooper().quitSafely();
        removeMessages(MESSAGE_CHECK);
    }

    private SimpleArrayMap<String, SparseIntArray> checkAndBrevent() {

        mUser = HideApi.getCurrentUser();
        mLauncher = HideApi.getLauncher(mUser);

        ServerLog.d("check and brevent");

        ActivitiesHolder runningActivities = getRunningActivities();
        SimpleArrayMap<String, Integer> running = runningActivities.running;
        Set<String> top = runningActivities.top;
        Set<String> home = runningActivities.home;

        Set<String> recent = getRecentPackages();
        recent.addAll(top);
        recent.addAll(home);

        if (Log.isLoggable(ServerLog.TAG, Log.DEBUG)) {
            ServerLog.d("running: " + running);
            ServerLog.d("top: " + top);
            ServerLog.d("home: " + home);
            ServerLog.d("current: " + mPackageName);
            ServerLog.d("recent: " + recent);
        }

        Collection<String> blocking = new ArraySet<>();
        Collection<String> noRecent = new ArraySet<>();
        Collection<String> standby = new ArraySet<>();

        Set<String> services = new ArraySet<>(mServices);
        mServices.clear();

        SimpleArrayMap<String, SparseIntArray> processes = getRunningProcesses(running);
        int size = processes.size();
        int now = TimeUtils.now();
        boolean checkLater = false;
        int timeout = mConfiguration.timeout;
        for (int i = 0; i < size; ++i) {
            String packageName = processes.keyAt(i);
            SparseIntArray status = processes.valueAt(i);
            if (mBrevent.contains(packageName)) {
                int inactive = BreventStatus.getInactive(status);
                if (Log.isLoggable(ServerLog.TAG, Log.DEBUG)) {
                    ServerLog.d("inactive for " + packageName + ": " + (inactive == 0 ? 0 : now - inactive) + ", timeout: " + timeout);
                }
                if (inactive == 0) {
                    blocking.add(packageName);
                } else {
                    services.remove(packageName);
                    if (timeout > 0) {
                        if (now - inactive > timeout) {
                            blocking.add(packageName);
                        } else {
                            checkLater = true;
                        }
                    }
                }
                if (!recent.contains(packageName)) {
                    noRecent.add(packageName);
                }
                if (BreventStatus.isStandby(status)) {
                    standby.add(packageName);
                }
            }
        }


        Set<String> unsafe = new ArraySet<>();
        SimpleArrayMap<String, Set<String>> dependencies = HideApi.getDependencies(getCacheDir(), mUser);

        if (Log.isLoggable(ServerLog.TAG, Log.DEBUG)) {
            size = dependencies.size();
            for (int i = 0; i < size; ++i) {
                ServerLog.d(dependencies.keyAt(i) + " depended by " + dependencies.valueAt(i));
            }
        }

        for (String packageName : blocking) {
            Set<String> packageNames = dependencies.get(packageName);
            if (packageNames != null) {
                packageNames.removeAll(blocking);
                if (!packageNames.isEmpty()) {
                    unsafe.add(packageName);
                    ServerLog.i("won't block " + packageName + " due do dependency by " + packageNames);
                }
            }
        }

        Set<String> back = new ArraySet<>(mBack);
        if (!HideApi.isCharging()) {
            mBack.clear();
        }
        blocking.addAll(services);
        blocking.addAll(back);
        blocking.removeAll(top);
        blocking.removeAll(home);
        blocking.removeAll(unsafe);
        if (mPackageName != null) {
            blocking.remove(mPackageName);
        }

        if (Log.isLoggable(ServerLog.TAG, Log.DEBUG)) {
            ServerLog.d("back: " + back);
            ServerLog.d("service: " + services);
            ServerLog.d("blocking: " + blocking);
            ServerLog.d("noRecent: " + noRecent);
            ServerLog.d("unsafe: " + unsafe);
            ServerLog.d("final blocking: " + blocking);
        }

        if (!blocking.isEmpty() && mConfiguration.allowGcm && Log.isLoggable(ServerLog.TAG, Log.DEBUG)) {
            ServerLog.d("gcm: " + mGcm);
        }

        for (String packageName : blocking) {
            if (packageName.equals(mPackageName)) {
                // shouldn't happen
                continue;
            }
            if (mConfiguration.appopsBackground) {
                HideApi.setAllowBackground(packageName, false, mUser);
            }
            if (services.contains(packageName)) {
                if (mConfiguration.appopsNotification) {
                    HideApi.setAllowNotification(packageName, false, mUser);
                }
                forceStop(packageName, "(service)");
            } else {
                brevent(packageName, standby, noRecent.contains(packageName));
            }
        }

        removeMessages(MESSAGE_CHECK);
        if (!screen) {
            if (checkLater) {
                checkAgain(processes);
            } else {
                checkRecentAgain(processes);
            }
        }

        if (!hasNotificationCancelAll) {
            checkChangedManually();
        }
        return processes;
    }

    private void checkChangedManually() {
        Set<String> packageNames = new ArraySet<>();
        for (PackageInfo packageInfo : HideApi.getInstalledPackages(mUser)) {
            packageNames.add(packageInfo.packageName);
        }
        if (!packageNames.equals(mInstalled)) {
            for (String packageName : packageNames) {
                if (!mInstalled.contains(packageName)) {
                    onAddPackage(packageName);
                }
            }
            for (String packageName : mInstalled) {
                if (!packageNames.contains(packageName)) {
                    onRemovePackage(packageName);
                }
            }
            PackageInfo packageInfo = HideApi.getPackageInfo(BuildConfig.APPLICATION_ID, 0, mUser);
            if (packageInfo == null) {
                onUninstall();
            } else if (!mVersionName.equals(packageInfo.versionName)) {
                onUpdated(packageInfo.versionName);
            }
        }
    }

    private void brevent(String packageName, Collection<String> standby, boolean noRecent) {
        switch (mConfiguration.method) {
            case BreventConfiguration.BREVENT_METHOD_FORCE_STOP_ONLY:
                forceStop(packageName, "(forceStop)");
                break;
            case BreventConfiguration.BREVENT_METHOD_STANDBY_ONLY:
                standby(standby.contains(packageName), packageName);
                break;
            case BreventConfiguration.BREVENT_METHOD_STANDBY_FORCE_STOP:
                if (noRecent) {
                    forceStop(packageName, "(noRecent)");
                } else {
                    standby(standby.contains(packageName), packageName);
                }
                break;
            default:
                break;
        }
    }

    private void forceStop(String packageName, String reason) {
        HideApi.forceStopPackage(packageName, reason, mUser);
        setStopped(packageName, true);
    }

    private void standby(boolean inactive, String packageName) {
        if (!inactive) {
            HideApi.setAppInactive(packageName, true, mUser);
        }
        setStopped(packageName, false);
    }

    private void setStopped(String packageName, boolean current) {
        if (mConfiguration.allowGcm && mGcm.contains(packageName)) {
            if (current) {
                HideApi.setStopped(packageName, false, mUser);
            }
        } else {
            if (!current) {
                HideApi.setStopped(packageName, true, mUser);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getRecentPackages() {
        Set<String> recent = new ArraySet<>();
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            List<ActivityManager.RecentTaskInfo> recentTasks;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recentTasks = am.getRecentTasks(Integer.MAX_VALUE, RECENT_FLAGS, mUser).getList();
            } else {
                recentTasks = HideApiOverrideM.getRecentTasks(am, Integer.MAX_VALUE, RECENT_FLAGS, mUser);
            }
            long minActiveTime = System.currentTimeMillis() - NAX_SURVIVE_TIME;
            for (ActivityManager.RecentTaskInfo recentTask : recentTasks) {
                if (recentTask.baseIntent != null && recentTask.baseIntent.getComponent() != null) {
                    if (HideApiOverride.getLastActiveTime(recentTask) >= minActiveTime || isFreeForm(recentTask)) {
                        recent.add(recentTask.baseIntent.getComponent().getPackageName());
                    }
                }
            }
            return recent;
        } catch (RemoteException e) {
            ServerLog.d("Can't get recent tasks", e);
            throw new UnsupportedOperationException(e);
        }
    }

    private boolean isFreeForm(ActivityManager.RecentTaskInfo taskInfo) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && HideApiOverrideN.isFreeForm(taskInfo);
    }

    private void checkAgain(SimpleArrayMap<String, SparseIntArray> processes) {
        int size = processes.size();
        for (int i = 0; i < size; ++i) {
            String packageName = processes.keyAt(i);
            SparseIntArray status = processes.valueAt(i);
            if (mBrevent.contains(packageName) && !BreventStatus.isStandby(status) && BreventStatus.getInactive(status) > 0) {
                checkLater(CHECK_LATER_SCREEN_OFF);
                break;
            }
        }
    }

    private void checkRecentAgain(SimpleArrayMap<String, SparseIntArray> processes) {
        int size = processes.size();
        for (int i = 0; i < size; ++i) {
            String packageName = processes.keyAt(i);
            if (mBrevent.contains(packageName)) {
                sendEmptyMessageDelayed(MESSAGE_CHECK, CHECK_LATER_RECENT);
                break;
            }
        }
    }

    private void checkChanged() {
        removeMessages(MESSAGE_CHECK_CHANGED);
        Set<String> packageNames = new ArraySet<>(mChanged);
        mChanged.clear();
        for (String packageName : packageNames) {
            if (!HideApi.isPackageAvailable(packageName, mUser)) {
                if (mInstalled.remove(packageName)) {
                    onRemovePackage(packageName);
                }
            } else if (mInstalled.add(packageName)) {
                onAddPackage(packageName);
            }
        }
        if (packageNames.contains(BuildConfig.APPLICATION_ID)) {
            PackageInfo packageInfo = HideApi.getPackageInfo(BuildConfig.APPLICATION_ID, 0, mUser);
            if (packageInfo == null) {
                onUninstall();
            } else if (!mVersionName.equals(packageInfo.versionName)) {
                onUpdated(packageInfo.versionName);
            }
        }
        removeMessages(MESSAGE_CHECK_CHANGED);
    }

    private void onUpdated(String versionName) {
        long live = System.currentTimeMillis() - mTime;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(live);
        ServerLog.i("version changed from " + mVersionName + " to " + versionName + ", live " + DateUtils.formatElapsedTime(seconds));
        if (live < CHECK_LATER_UPDATE) {
            ServerLog.i("live too short, update later");
            sendEmptyMessageDelayed(MESSAGE_UPDATE, CHECK_LATER_APPS - live);
        } else {
            quitSafely();
        }
    }

    private void onUninstall() {
        ServerLog.i("uninstalled");
        //noinspection ConstantConditions
        Looper.myLooper().quit();
        for (String packageName : mBrevent) {
            unblock(packageName);
        }
        if (!getBreventConf().delete()) {
            ServerLog.w("Can't remove brevent conf");
        }
        if (!getBreventList().delete()) {
            ServerLog.w("Can't remove brevent list");
        }
    }

    private void onRemovePackage(String packageName) {
        ServerLog.i("remove package " + packageName);
        mGcm.remove(packageName);
        mBrevent.remove(packageName);
        updateBreventIfNeeded(false, packageName);
    }

    private void onAddPackage(String packageName) {
        ServerLog.i("add package " + packageName);
        updateBreventIfNeeded(true, packageName);
        if (HideApi.isGcm(packageName, mUser)) {
            mGcm.add(packageName);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleEvent(int tag, int tid, SimpleArrayMap<String, Object> event) {
        switch (tag) {
            case EventTag.AM_FOCUSED_ACTIVITY:
                // for check schedule, back open launcher, back background
                handleEventAmFocusedActivity(tid, event);
                break;
            case EventTag.AM_NEW_INTENT:
                // for back open launcher
                handleEventAmNewIntent(tid, event);
                break;
            case EventTag.AM_PAUSE_ACTIVITY:
                // for back open launcher, back background
                handleEventAmPauseActivity(event);
                break;
            case EventTag.AM_PROC_START:
                handleEventAmProcStart(event);
                break;
            case EventTag.NOTIFICATION_CANCEL_ALL:
                // for package changed, restart brevent server,
                hasNotificationCancelAll = true;
                handleEventNotificationCancelAll(event);
                break;
            case EventTag.POWER_SCREEN_STATE:
                // screen is off
                handleEventPowerScreenState(event);
                break;
            default:
                break;
        }
    }

    private void handleEventAmPauseActivity(SimpleArrayMap<String, Object> event) {
        // am_pause_activity (User|1|5),(Token|1|5),(Component Name|3)
        if (MOVE_TASK_TO_BACK.equals(mFocusReason) || OPEN_LAUNCHER.equals(mFocusReason)) {
            String packageName = getPackageName((String) event.get("componentName"));
            if (packageName != null && mBrevent.contains(packageName)) {
                mBack.add(packageName);
                checkLaterIfLater(CHECK_LATER_BACK);
                ServerLog.i("will check " + packageName + "(" + mFocusReason + ")");
            }
        }
        homeTid = 0;
    }

    private void handleEventAmNewIntent(int tid, SimpleArrayMap<String, Object> event) {
        // am_new_intent (User|1|5),(Token|1|5),(Task ID|1|5),(Component Name|3),(Action|3),(MIME Type|3),(URI|3),(Flags|1|5)
        String componentName = (String) event.get("componentName");
        String packageName = getPackageName(componentName);
        int flags = (int) event.get("flags");
        if (mLauncher.equals(packageName)) {
            if ((flags & HOME_INTENT_FLAGS) == HOME_INTENT_FLAGS) {
                if (possibleHomeTid == tid) {
                    repeatCount++;
                } else {
                    possibleHomeTid = tid;
                    repeatCount = 1;
                }
                if (repeatCount >= 0x7) {
                    mHomeTid = tid;
                    repeatCount = 0;
                    ServerLog.i("reset home tid to " + mHomeTid);
                }
            }
            homeTid = tid;
        }
    }

    private void handleEventAmFocusedActivity(int tid, SimpleArrayMap<String, Object> event) {
        // am_focused_activity (User|1|5),(Component Name|3),(Reason|3)
        String componentName = (String) event.get("componentName");
        String packageName = getPackageName(componentName);
        if (packageName == null) {
            return;
        }
        mBack.remove(packageName);
        mServices.remove(packageName);
        if (mBrevent.contains(packageName)) {
            unblock(packageName);
        }
        if (packageName.equals(mPackageName)) {
            return;
        }
        mPackageName = packageName;
        String reason = (String) event.get("reason");
        // reason since api-24
        if (reason == null) {
            reason = "(no reason)";
        }
        mFocusReason = null;
        if (reason.startsWith(MOVE_TASK_TO_BACK)) {
            if (mConfiguration.backMove) {
                mFocusReason = MOVE_TASK_TO_BACK;
            }
        } else if (mLauncher.equals(packageName)) {
            if (mConfiguration.backHome && isBackHome(tid)) {
                mFocusReason = OPEN_LAUNCHER;
            }
        }
        if (mLauncher.equals(packageName)) {
            checkLaterIfLater(CHECK_LATER_HOME);
        } else {
            checkLaterIfLater(CHECK_LATER_APPS);
        }
    }

    private boolean isBackHome(int tid) {
        if (Log.isLoggable(ServerLog.TAG, Log.VERBOSE)) {
            ServerLog.v("tid: " + tid + ", homeTid: " + homeTid + ", mHomeTid: " + mHomeTid + ", possibleHomeTid: " + possibleHomeTid);
        }
        if (homeTid == 0) {
            return false;
        }
        if (mHomeTid > 0) {
            return mHomeTid != tid;
        }
        if (possibleHomeTid > 0) {
            return possibleHomeTid != tid;
        }
        return homeTid == tid;
    }

    private void handleEventPowerScreenState(SimpleArrayMap<String, Object> event) {
        // power_screen_state (offOrOn|1|5),(becauseOfUser|1|5),(totalTouchDownTime|2|3),(touchCycles|1|1)
        int offOrOn = (int) event.get("offOrOn");
        if (offOrOn == 1) {
            screen = true;
            checkLaterIfLater(CHECK_LATER_HOME);
        } else {
            screen = false;
            checkLater(0);
        }
    }

    private void checkLaterIfLater(int later) {
        if (mConfiguration.mode == BreventConfiguration.BREVENT_MODE_IMMEDIATE) {
            checkLater(0);
        } else {
            checkLater(later);
        }
    }

    private void checkLater(int later) {
        if (later <= CHECK_LATER_SERVICE) {
            removeMessages(MESSAGE_CHECK);
        }
        sendEmptyMessageDelayed(MESSAGE_CHECK, later);
    }

    private void handleEventNotificationCancelAll(SimpleArrayMap<String, Object> event) {
        // notification_cancel_all (uid|1|5),(pid|1|5),(pkg|3),(userid|1|5),(required_flags|1),(forbidden_flags|1),(reason|1|5),(listener|3)
        String packageName = (String) event.get("pkg");
        Integer reason = (Integer) event.get("reason");
        // 5: REASON_PACKAGE_CHANGED
        if (packageName != null && (reason == null || reason == 0x5)) {
            mChanged.add(packageName);
            checkChangedLater();
        }
    }

    private void checkChangedLater() {
        removeMessages(MESSAGE_CHECK_CHANGED);
        sendEmptyMessageDelayed(MESSAGE_CHECK_CHANGED, 0x1000);
    }

    private static String getPackageName(String componentName) {
        if (componentName != null) {
            ComponentName component = ComponentName.unflattenFromString(componentName);
            if (component != null) {
                return component.getPackageName();
            }
        }
        return null;
    }

    private void handleEventAmProcStart(SimpleArrayMap<String, Object> event) {
        // am_proc_start (User|1|5),(PID|1|5),(UID|1|5),(Process Name|3),(Type|3),(Component|3)
        String type = (String) event.get("type");
        String component = (String) event.get("component");
        String packageName = getPackageName(component);

        if ("activity".equals(type)) {
            mBack.remove(packageName);
            mServices.remove(packageName);
            if (mBrevent.contains(packageName)) {
                unblock(packageName);
            }
        } else if ("service".equals(type)) {
            if (mBrevent.contains(packageName)) {
                mServices.add(packageName);
                checkLaterIfLater(CHECK_LATER_SERVICE);
            }
        }
    }

    private void handleRequest(BreventProtocol request) {
        int action = request.getAction();
        if (Log.isLoggable(ServerLog.TAG, Log.VERBOSE)) {
            ServerLog.v("action: " + request.getAction() + ", request: " + request);
        }
        if (action == BreventProtocol.STATUS_REQUEST) {
            handleStatus(mToken);
        } else if (request instanceof BreventToken) {
            if (((BreventToken) request).getToken().equals(mToken)) {
                switch (action) {
                    case BreventProtocol.UPDATE_BREVENT:
                        handleUpdateBrevent((BreventPackages) request);
                        break;
                    case BreventProtocol.CONFIGURATION:
                        handleUpdateConfiguration((BreventConfiguration) request);
                        break;
                    default:
                        break;

                }
            } else {
                ServerLog.w("invalid token, action: " + request.getAction() + ", request: " + request);
            }
        }
    }

    private boolean handleBrevent(boolean brevent, String packageName) {
        if (brevent && !mBrevent.contains(packageName)) {
            mBrevent.add(packageName);
            return true;
        } else if (!brevent && mBrevent.contains(packageName)) {
            mBrevent.remove(packageName);
            unblock(packageName);
            return true;
        } else {
            return false;
        }
    }

    private void unblock(String packageName) {
        if (mConfiguration.appopsNotification) {
            HideApi.setAllowNotification(packageName, true, mUser);
        }
        HideApi.setAppInactive(packageName, false, mUser);
        HideApi.setStopped(packageName, false, mUser);
        if (mConfiguration.appopsBackground) {
            HideApi.setAllowBackground(packageName, true, mUser);
        }
    }

    private void handleUpdateBrevent(BreventPackages request) {
        boolean brevent = request.brevent;
        Set<String> updated = new ArraySet<>();
        for (String packageName : request.packageNames) {
            if (mInstalled.contains(packageName) && handleBrevent(brevent, packageName)) {
                updated.add(packageName);
            }
        }
        if (!updated.isEmpty()) {
            saveBreventListLater();
        }
        request.packageNames.clear();
        request.packageNames.addAll(updated);
        sendBroadcast(request);

        if (brevent && !updated.isEmpty()) {
            checkLaterIfLater(CHECK_LATER_USER);
        }
    }

    private void saveBreventConfLater() {
        removeMessages(MESSAGE_SAVE_BREVENT_CONF);
        sendEmptyMessageDelayed(MESSAGE_SAVE_BREVENT_CONF, 0x1000);
    }

    private void saveBreventConf() {
        removeMessages(MESSAGE_SAVE_BREVENT_CONF);
        File file = getBreventConfBak();
        try (
                PrintWriter pw = new PrintWriter(new FileWriter(file))
        ) {
            mConfiguration.write(pw);
        } catch (IOException e) {
            ServerLog.w("Can't save configuration", e);
        }
        if (!file.renameTo(getBreventConf())) {
            ServerLog.w("Can't save configuration");
        }
    }

    private void saveBreventListLater() {
        removeMessages(MESSAGE_SAVE_BREVENT_LIST);
        sendEmptyMessageDelayed(MESSAGE_SAVE_BREVENT_LIST, 0x1000);
    }

    private void saveBreventList() {
        removeMessages(MESSAGE_SAVE_BREVENT_LIST);
        File file = getBreventListBak();
        try (
                PrintWriter pw = new PrintWriter(new FileWriter(file))
        ) {
            for (String key : mBrevent) {
                pw.println(key);
            }
        } catch (IOException e) {
            ServerLog.w("Can't save brevent list", e);
        }
        if (!file.renameTo(getBreventList())) {
            ServerLog.w("Can't save brevent list");
        }
    }

    private void updateBreventIfNeeded(boolean brevent, String packageName) {
        if (mConfiguration.autoUpdate) {
            handleBrevent(brevent, packageName);
            saveBreventListLater();
        }
    }

    private void handleUpdateConfiguration(BreventConfiguration request) {
        if (mConfiguration.appopsBackground != request.appopsBackground && !request.appopsBackground) {
            for (String packageName : mBrevent) {
                HideApi.setAllowBackground(packageName, true, mUser);
            }
        }
        if (mConfiguration.appopsNotification != request.appopsNotification && !request.appopsNotification) {
            for (String packageName : mBrevent) {
                HideApi.setAllowNotification(packageName, true, mUser);
            }
        }
        if (mConfiguration.update(request)) {
            saveBreventConfLater();
        }
        sendBroadcast(mConfiguration);
    }

    private void handleStatus(UUID token) {
        checkAndBrevent();
        SimpleArrayMap<String, Integer> running = getRunningActivities().running;
        BreventStatus response = new BreventStatus(token, mBrevent, getRunningProcesses(running));
        sendBroadcast(response);
        removeMessages(MESSAGE_REQUEST_STATUS);
        checkAlive();
    }

    private ActivitiesHolder getRunningActivities() {
        if (Log.isLoggable(ServerLog.TAG, Log.VERBOSE)) {
            ServerLog.v("get running activities");
        }

        ActivitiesHolder holder = new ActivitiesHolder();
        List<TaskRecord> taskRecords = HideApi.getRunningActivities(getCacheDir(), mUser);
        if (taskRecords.isEmpty()) {
            ServerLog.e("Can't check running activities");
            return holder;
        }
        boolean onHome = taskRecords.get(0).isHome();
        int now = TimeUtils.now();
        for (TaskRecord taskRecord : taskRecords) {
            String packageName = taskRecord.packageName;
            if (taskRecord.isHome()) {
                holder.home.add(packageName);
            } else {
                if (!onHome && taskRecord.top) {
                    holder.top.add(packageName);
                }
                if (!holder.running.containsKey(packageName)) {
                    holder.running.put(packageName, now - taskRecord.inactive);
                }
            }
        }
        return holder;
    }

    private void sendBroadcast(BreventProtocol response) {
        final Intent intent = new Intent(BreventIntent.ACTION_BREVENT);
        BreventProtocol.wrap(intent, response);
        HideApi.PendingResult pendingResult = HideApi.sendBroadcast(intent, mUser);
        String resultData;
        if (pendingResult != null && (resultData = pendingResult.getResultData()) != null) {
            try {
                UUID token = UUID.fromString(resultData);
                if (!BreventToken.EMPTY_TOKEN.equals(token) && !token.equals(mToken)) {
                    mToken = token;
                    removeMessages(MESSAGE_EXIT);
                }
            } catch (IllegalArgumentException e) {
                ServerLog.w("cannot parse " + resultData + " as uuid", e);
            }
        }
    }

    private File getBrevent(String suffix) {
        return new File(mDataDir, BuildConfig.APPLICATION_ID + suffix);
    }

    private File getBreventList() {
        return getBrevent(".list");
    }

    private File getBreventListBak() {
        return getBrevent(".list.bak");
    }

    private File getBreventConf() {
        return getBrevent(".conf");
    }

    private File getBreventConfBak() {
        return getBrevent(".conf.lock");
    }

    private File getCacheDir() {
        File file = new File(mDataDir, "cache");
        if (!file.isDirectory() && !file.mkdirs()) {
            return new File(mDataDir);
        }
        return file;
    }

    private SimpleArrayMap<String, SparseIntArray> getRunningProcesses(SimpleArrayMap<String, Integer> running) {
        if (Log.isLoggable(ServerLog.TAG, Log.VERBOSE)) {
            ServerLog.v("get running processes");
        }
        SimpleArrayMap<String, SparseIntArray> processes = new SimpleArrayMap<>();
        for (ActivityManager.RunningAppProcessInfo process : HideApi.getRunningAppProcesses()) {
            if (process.uid < Process.FIRST_APPLICATION_UID) {
                continue;
            }
            int processState = HideApiOverride.getProcessState(process);
            for (String packageName : process.pkgList) {
                SparseIntArray status = processes.get(packageName);
                if (status != null) {
                    int oldValue = status.get(processState, 1);
                    status.put(processState, oldValue + 1);
                } else {
                    status = new SparseIntArray();
                    status.put(processState, 1);
                    status.put(BreventStatus.PROCESS_STATE_IDLE, HideApi.getAppInactive(packageName, mUser) ? 1 : 0);
                    Integer inactive = running.get(packageName);
                    status.put(BreventStatus.PROCESS_STATE_INACTIVE, inactive == null ? 0 : inactive);
                    status.put(BreventStatus.PROCESS_STATE_PERSISTENT, HideApiOverride.isPersistent(process) ? 1 : 0);
                    processes.put(packageName, status);
                }
            }
        }
        return processes;
    }

    private static String getDataDir(int owner) {
        int uid = HideApiOverride.uidForData(Process.myUid());
        IPackageManager packageManager = HideApi.getPackageManager();
        String[] packageNames;
        try {
            packageNames = packageManager.getPackagesForUid(uid);
        } catch (RemoteException e) {
            ServerLog.e("Can't find package for " + uid, e);
            return null;
        }
        if (packageNames != null) {
            for (String packageName : packageNames) {
                try {
                    return packageManager.getPackageInfo(packageName, 0, owner).applicationInfo.dataDir;
                } catch (RemoteException e) {
                    ServerLog.w("Can't find package " + packageName + " for " + owner, e);
                }
            }
        }
        ServerLog.e("Can't find package for " + uid);
        return null;
    }

    public static void main(String[] args) throws IOException {
        ServerLog.i("Brevent Server " + BuildConfig.VERSION_NAME + " started");
        Looper.prepare();

        BreventServer breventServer = new BreventServer();

        CountDownLatch eventLatch = new CountDownLatch(0x1);
        BreventEvent breventEvent = new BreventEvent(breventServer, eventLatch);
        Thread eventThread = new Thread(breventEvent);
        eventThread.start();

        CountDownLatch socketLatch = new CountDownLatch(0x1);
        ServerSocket serverSocket = new ServerSocket(BreventProtocol.PORT, 0, BreventProtocol.HOST);
        serverSocket.setReuseAddress(true);
        Thread socketThread = new Thread(new BreventSocket(breventServer, serverSocket, socketLatch));
        socketThread.start();

        Looper.loop();

        breventEvent.quit();
        EventLog.writeEvent(42, 0xfee1dead);

        try {
            eventLatch.await(MAX_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            ServerLog.w("Can't await event in " + MAX_TIMEOUT + "s", e);
        }

        serverSocket.close();
        try {
            socketLatch.await();
        } catch (InterruptedException e) {
            ServerLog.w("Can't await socket in " + MAX_TIMEOUT + "s", e);
        }
        long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - breventServer.mTime);
        ServerLog.i("Brevent Server " + BuildConfig.VERSION_NAME + " completed, live " + DateUtils.formatElapsedTime(seconds));
    }

    private static class ActivitiesHolder {
        Set<String> top = new ArraySet<>();
        Set<String> home = new ArraySet<>();
        SimpleArrayMap<String, Integer> running = new SimpleArrayMap<>();
    }

}