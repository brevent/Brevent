package me.piebridge.brevent.server;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
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

    public static final int MESSAGE_EVENT = 1;
    public static final int MESSAGE_REQUEST_STATUS = 2;
    public static final int MESSAGE_REQUEST_MANAGE = 3;

    private static final int MESSAGE_SAVE_BREVENT_LIST = 4;
    private static final int MESSAGE_SAVE_BREVENT_CONF = 5;

    private static final int MESSAGE_CHECK = 6;
    private static final int MESSAGE_CHECK_CHANGED = 7;

    private static final int MAX_TIMEOUT = 30;

    private volatile boolean screen = true;

    private final String mDataDir;

    private UUID mToken;

    private final String mVersionName;

    private final Set<String> mInstalled = new ArraySet<>();
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

    private static final int CHECK_LATER_USER = 3000;
    private static final int CHECK_LATER_BACK = 3000;
    private static final int CHECK_LATER_HOME = 3000;
    private static final int CHECK_LATER_SERVICE = 30000;
    private static final int CHECK_LATER_APPS = 60000;
    private static final int CHECK_LATER_SCREEN_OFF = 60000;

    private static final int RECENT_FLAGS = ActivityManager.RECENT_IGNORE_UNAVAILABLE | ActivityManager.RECENT_WITH_EXCLUDED;
    private static final int HOME_INTENT_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;

    private BreventServer() throws IOException {
        super();

        PackageInfo packageInfo = HideApi.getPackageInfo(BuildConfig.APPLICATION_ID);
        mVersionName = packageInfo.versionName;
        if (!BuildConfig.VERSION_NAME.equals(mVersionName)) {
            ServerLog.w("version unmatched, running: " + BuildConfig.VERSION_NAME + ", installed: " + mVersionName);
        }

        mUser = HideApi.getCurrentUser();
        ServerLog.d("brevent user: " + mUser + ", uid: " + Process.myUid());

        mDataDir = getDataDir(HideApi.USER_OWNER);
        if (mDataDir == null) {
            throw new FileNotFoundException("cannot find brevent data directory");
        }

        ServerLog.d("brevent data directory: " + mDataDir);
        loadBreventList();
        loadBreventConf();

        mServices = new ArraySet<>();
        mChanged = new ArraySet<>();
        mBack = new ArraySet<>();

        mLauncher = HideApi.getLauncher();

        handleStatus(BreventToken.EMPTY_TOKEN);

        check();
    }

    private void loadBreventConf() {
        File file = getBreventConf();
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
        }
        ServerLog.d("loaded brevent conf");
    }

    private void loadBreventList() {
        for (PackageInfo packageInfo : HideApi.getInstalledPackages(HideApi.USER_OWNER)) {
            mInstalled.add(packageInfo.packageName);
        }
        File file = getBreventList();
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
        }
        ServerLog.d("loaded brevent list");
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
                check();
                break;
        }
    }

    private void check() {
        removeMessages(MESSAGE_CHECK);


        Set<String> services = new ArraySet<>(mServices);
        mServices.clear();

        Set<String> back = new ArraySet<>(mBack);
        if (!HideApi.isCharging()) {
            mBack.clear();
        }

        mUser = HideApi.getCurrentUser();
        mLauncher = HideApi.getLauncher();

        ServerLog.d("checking");

        ActivitiesHolder runningActivities = getRunningActivities();
        SimpleArrayMap<String, Integer> running = runningActivities.running;
        Set<String> top = runningActivities.top;
        Set<String> home = runningActivities.home;

        Set<String> recent = getRecentPackages();

        if (!BuildConfig.RELEASE) {
            ServerLog.d("running: " + running);
            ServerLog.d("top: " + top);
            ServerLog.d("home: " + home);
        }

        SimpleArrayMap<String, SparseIntArray> processes = getRunningProcesses(running);
        int size = processes.size();

        Collection<String> blocking = new ArraySet<>();
        Collection<String> noRecent = new ArraySet<>();
        boolean checkLater = false;
        int timeout = mConfiguration.timeout;
        for (int i = 0; i < size; ++i) {
            String packageName = processes.keyAt(i);
            SparseIntArray status = processes.valueAt(i);
            if (mBrevent.contains(packageName)) {
                if (!recent.contains(packageName)) {
                    noRecent.add(packageName);
                }
                if (!BreventStatus.isStandby(status)) {
                    int inactive = BreventStatus.getInactive(status);
                    if (inactive == 0) {
                        blocking.add(packageName);
                    } else if (timeout > 0) {
                        if (inactive > timeout) {
                            blocking.add(packageName);
                        } else {
                            checkLater = true;
                        }
                    }
                }
            }
        }

        if (!BuildConfig.RELEASE) {
            ServerLog.d("blocking: " + blocking);
            ServerLog.d("back: " + back);
            ServerLog.d("noRecent: " + noRecent);
        }

        blocking.addAll(services);
        blocking.addAll(back);
        blocking.removeAll(top);
        blocking.removeAll(home);

        Set<String> unsafe = new ArraySet<>();
        SimpleArrayMap<String, Set<String>> dependencies = HideApi.getDependencies(getCacheDir());

        if (!BuildConfig.RELEASE) {
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
                    ServerLog.d("won't block " + packageName + " due do dependency by " + packageNames);
                }
            }
        }

        blocking.removeAll(unsafe);

        if (!BuildConfig.RELEASE) {
            ServerLog.d("unsafe: " + unsafe);
            ServerLog.d("final blocking: " + blocking);
        }

        for (String packageName : blocking) {
            block(packageName);
            if (noRecent.contains(packageName)) {
                HideApi.forceStopPackage(packageName, "(no recent)");
            }
        }

        removeMessages(MESSAGE_CHECK);
        if (!screen && checkLater) {
            checkAgain(processes);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getRecentPackages() {
        Set<String> recent = new ArraySet<>();
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            // system ui only show about 10 recent
            List<ActivityManager.RecentTaskInfo> recentTasks;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recentTasks = am.getRecentTasks(Integer.MAX_VALUE, RECENT_FLAGS, mUser).getList();
            } else {
                recentTasks = HideApiM.getRecentTasks(am, Integer.MAX_VALUE, RECENT_FLAGS, mUser);
            }
            for (ActivityManager.RecentTaskInfo recentTask : recentTasks) {
                if (recentTask.baseIntent != null) {
                    ComponentName componentName = recentTask.baseIntent.getComponent();
                    if (componentName != null) {
                        recent.add(componentName.getPackageName());
                    }
                }
            }
            return recent;
        } catch (RemoteException e) {
            ServerLog.d("Can't get recent tasks");
            throw new UnsupportedOperationException(e);
        }
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

    private void checkChanged() {
        removeMessages(MESSAGE_CHECK_CHANGED);
        Set<String> packageNames = new ArraySet<>(mChanged);
        mChanged.clear();
        for (String packageName : packageNames) {
            if (!HideApi.isPackageAvailable(packageName)) {
                if (mInstalled.remove(packageName)) {
                    ServerLog.d("remove package " + packageName);
                    updateBreventIfNeeded(false, packageName);
                }
            } else if (mInstalled.add(packageName)) {
                ServerLog.d("add package " + packageName);
                updateBreventIfNeeded(true, packageName);
            }
        }
        if (packageNames.contains(BuildConfig.APPLICATION_ID)) {
            PackageInfo packageInfo = HideApi.getPackageInfo(BuildConfig.APPLICATION_ID);
            if (packageInfo == null) {
                //noinspection ConstantConditions
                Looper.myLooper().quit();
                if (!getBreventConf().delete()) {
                    ServerLog.d("Can't remove brevent conf");
                }
                if (!getBreventList().delete()) {
                    ServerLog.d("Can't remove brevent list");
                }
            } else if (!mVersionName.equals(packageInfo.versionName)) {
                ServerLog.d("version changed from " + mVersionName + " to " + packageInfo.versionName);
                //noinspection ConstantConditions
                Looper.myLooper().quitSafely();
                removeMessages(MESSAGE_CHECK);
            }
        }
        removeMessages(MESSAGE_CHECK_CHANGED);
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
                ServerLog.d("will check " + packageName + "(" + mFocusReason + ")");
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
                    ServerLog.i("reset home tid to " + mHomeTid);
                }
            }
            homeTid = tid;
        }
    }

    private void handleEventAmFocusedActivity(int tid, SimpleArrayMap<String, Object> event) {
        // am_focused_activity (User|1|5),(Component Name|3),(Reason|3)
        String reason = (String) event.get("reason");
        // reason since api-24
        if (reason == null) {
            reason = "(no reason)";
        }
        String componentName = (String) event.get("componentName");
        String packageName = getPackageName(componentName);
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
        if (!BuildConfig.RELEASE) {
            ServerLog.d("tid: " + tid + ", homeTid: " + homeTid + ", mHomeTid: " + mHomeTid + ", possibleHomeTid: " + possibleHomeTid);
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
        removeMessages(MESSAGE_CHECK);
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
        if (!BuildConfig.RELEASE) {
            ServerLog.d("action: " + request.getAction() + ", request: " + request
                    + ", token: " + ((request instanceof BreventToken) ? ((BreventToken) request).getToken() : null)
                    + ", mToken: " + mToken);
        }
        if (action == BreventProtocol.STATUS_REQUEST) {
            handleStatus(mToken);
        } else if (request instanceof BreventToken && ((BreventToken) request).getToken().equals(mToken)) {
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

    private void block(String packageName) {
        HideApi.setInactive(packageName, true);
        if (mConfiguration.appopsBackground) {
            HideApi.setAllowBackground(packageName, false);
        }
        HideApi.setStopped(packageName, true);
    }

    private void unblock(String packageName) {
        if (mConfiguration.appopsBackground) {
            HideApi.setAllowBackground(packageName, true);
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
        if (mConfiguration.update(request)) {
            saveBreventConfLater();
        }
        sendBroadcast(mConfiguration);
    }

    private void handleStatus(UUID token) {
        SimpleArrayMap<String, Integer> running = getRunningActivities().running;
        SimpleArrayMap<String, SparseIntArray> processes = getRunningProcesses(running);
        BreventStatus response = new BreventStatus(token, mBrevent, processes, HideApi.getVpnPackages());
        sendBroadcast(response);
        removeMessages(MESSAGE_REQUEST_STATUS);
    }

    private ActivitiesHolder getRunningActivities() {
        if (!BuildConfig.RELEASE) {
            ServerLog.d("get running activities");
        }

        ActivitiesHolder holder = new ActivitiesHolder();
        List<TaskRecord> taskRecords = HideApi.getRunningActivities(getCacheDir());
        if (taskRecords.isEmpty()) {
            ServerLog.e("Can't check running activities");
            return holder;
        }
        TaskRecord firstTaskRecord = taskRecords.get(0);
        int firstStack = firstTaskRecord.stack;
        boolean onHome = firstTaskRecord.isHome();
        boolean fullscreen = firstTaskRecord.fullscreen;
        int now = TimeUtils.now();
        for (TaskRecord taskRecord : taskRecords) {
            String packageName = taskRecord.packageName;
            // collect top package
            if (taskRecord.top && !onHome && (!fullscreen || taskRecord.stack == firstStack)) {
                holder.top.add(packageName);
            }
            if (taskRecord.isHome()) {
                holder.home.add(packageName);
            } else if (!holder.running.containsKey(packageName)) {
                holder.running.put(packageName, now - taskRecord.inactive);
            }
        }
        return holder;
    }

    private void sendBroadcast(BreventProtocol response) {
        final Intent intent = new Intent(BreventIntent.ACTION_BREVENT);
        BreventProtocol.wrap(intent, response);
        HideApi.PendingResult pendingResult = HideApi.sendBroadcast(intent);
        String resultData;
        if (pendingResult != null && (resultData = pendingResult.getResultData()) != null) {
            try {
                UUID token = UUID.fromString(resultData);
                if (!BreventToken.EMPTY_TOKEN.equals(token)) {
                    mToken = token;
                }
            } catch (IllegalArgumentException e) {
                ServerLog.d("cannot parse " + resultData + " as uuid", e);
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
        if (!BuildConfig.RELEASE) {
            ServerLog.d("get running processes");
        }
        SimpleArrayMap<String, SparseIntArray> processes = new SimpleArrayMap<>();
        for (ActivityManager.RunningAppProcessInfo process : HideApi.getRunningAppProcesses()) {
            int processState = HideApi.getProcessState(process);
            for (String pkg : process.pkgList) {
                SparseIntArray status;
                int processIndex = processes.indexOfKey(pkg);
                if (processIndex >= 0) {
                    status = processes.valueAt(processIndex);
                    int oldValue = status.get(processState, 1);
                    status.put(processState, oldValue + 1);
                } else {
                    status = new SparseIntArray();
                    status.put(processState, 1);
                    status.put(BreventStatus.PROCESS_STATE_IDLE, HideApi.getInactive(pkg) ? 1 : 0);
                    int runningIndex = running.indexOfKey(pkg);
                    status.put(BreventStatus.PROCESS_STATE_INACTIVE, runningIndex >= 0 ? running.valueAt(runningIndex) : 0);
                    processes.put(pkg, status);
                }
            }
        }
        return processes;
    }

    private static String getDataDir(int owner) {
        int uid = Process.myUid();
        IPackageManager packageManager = AppGlobals.getPackageManager();
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
        ServerLog.d("Brevent Server " + BuildConfig.VERSION_NAME + " started");
        Looper.prepare();

        Handler handler = new BreventServer();

        CountDownLatch eventLatch = new CountDownLatch(0x1);
        BreventEvent breventEvent = new BreventEvent(handler, eventLatch);
        Thread eventThread = new Thread(breventEvent);
        eventThread.start();

        CountDownLatch socketLatch = new CountDownLatch(0x1);
        ServerSocket serverSocket = new ServerSocket(BreventProtocol.PORT, 0, BreventProtocol.HOST);
        serverSocket.setReuseAddress(true);
        Thread socketThread = new Thread(new BreventSocket(handler, serverSocket, socketLatch));
        socketThread.start();

        Looper.loop();

        breventEvent.quit();
        try {
            eventLatch.await(MAX_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            ServerLog.d("Can't await event in " + MAX_TIMEOUT + "s", e);
        }

        serverSocket.close();
        try {
            socketLatch.await();
        } catch (InterruptedException e) {
            ServerLog.d("Can't await socket in " + MAX_TIMEOUT + "s", e);
        }
        ServerLog.d("Brevent Server " + BuildConfig.VERSION_NAME + " completed");
    }

    private static class ActivitiesHolder {
        Set<String> top = new ArraySet<>();
        Set<String> home = new ArraySet<>();
        SimpleArrayMap<String, Integer> running = new SimpleArrayMap<>();
    }

}