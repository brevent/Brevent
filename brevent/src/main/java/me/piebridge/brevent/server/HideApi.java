package me.piebridge.brevent.server;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.usage.IUsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.v4.util.ArraySet;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;

import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IBatteryStats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.protocol.BreventIntent;

/**
 * Created by thom on 2016/12/28.
 */
class HideApi {

    static final int USER_OWNER = 0;

    private HideApi() {

    }

    // ActivityManager start

    public static void forceStopPackage(String packageName, String reason) throws HideApiException {
        try {
            ActivityManagerNative.getDefault().forceStopPackage(packageName, USER_OWNER);
            ServerLog.d("Force stop " + packageName + reason);
        } catch (RemoteException e) {
            throw new HideApiException("Can't force stop " + packageName, e);
        }
    }

    public static int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }

    public static List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses() {
        try {
            return ActivityManagerNative.getDefault().getRunningAppProcesses();
        } catch (RemoteException e) {
            throw new HideApiException("Can't get running app processes", e);
        }
    }

    public static PendingResult sendBroadcast(Intent intent) throws HideApiException {
        try {
            CountDownLatch latch = new CountDownLatch(0x1);
            IntentReceiver receiver = new IntentReceiver(latch);
            String[] requiredPermissions = new String[] {BreventIntent.PERMISSION_MANAGER};
            ActivityManagerNative.getDefault().broadcastIntent(null, intent, null, receiver,
                    0, null, null, requiredPermissions,
                    AppOpsManager.OP_NONE, null, true, false, USER_OWNER);
            try {
                latch.await(0xf, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                ServerLog.d("Can't send broadcast", e);
            }
            return receiver.getPendingResult();
        } catch (RemoteException e) {
            throw new HideApiException("Can't send broadcast", e);
        }
    }

    // ActivityManager end


    // PackageManager start

    public static List<PackageInfo> getInstalledPackages(int uid) {
        try {
            return AppGlobals.getPackageManager().getInstalledPackages(0, uid).getList();
        } catch (RemoteException e) {
            throw new HideApiException("Can't getInstalledPackages", e);
        }
    }

    public static boolean isPackageAvailable(String packageName) throws HideApiException {
        try {
            return AppGlobals.getPackageManager().isPackageAvailable(packageName, USER_OWNER);
        } catch (RemoteException e) {
            throw new HideApiException("Can't isPackageAvailable for " + packageName, e);
        }
    }

    public static String getLauncher() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo resolveInfo = AppGlobals.getPackageManager().resolveIntent(intent,
                    null, PackageManager.MATCH_DEFAULT_ONLY, USER_OWNER);
            return resolveInfo.activityInfo.packageName;
        } catch (RemoteException e) {
            throw new HideApiException("Can't get launcher", e);
        }
    }

    private static int getPackageUid(String packageName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return AppGlobals.getPackageManager().getPackageUid(packageName,
                        PackageManager.MATCH_UNINSTALLED_PACKAGES, USER_OWNER);
            } else {
                return getPackageUidDeprecated(packageName);
            }
        } catch (RemoteException e) {
            throw new HideApiException("Can't getPackageUid for " + packageName, e);
        }
    }

    @SuppressWarnings("deprecation")
    private static int getPackageUidDeprecated(String packageName) throws RemoteException {
        return AppGlobals.getPackageManager().getPackageUid(packageName, USER_OWNER);
    }

    public static PackageInfo getPackageInfo(String packageName) {
        try {
            return AppGlobals.getPackageManager().getPackageInfo(packageName, 0, HideApi.USER_OWNER);
        } catch (RemoteException e) {
            throw new HideApiException("Can't getPackageInfo", e);
        }
    }

    public static void setStopped(String packageName, boolean stopped) {
        try {
            AppGlobals.getPackageManager().setPackageStoppedState(packageName, stopped, HideApi.USER_OWNER);
        } catch (RemoteException e) {
            throw new HideApiException("Can't setPackageStoppedState for " + packageName, e);
        }
    }

    // PackageManager end


    // AppOpsService start

    private static boolean setMode(String packageName, int op, int mode) throws HideApiException {
        try {
            int packageUid = getPackageUid(packageName);
            if (packageUid < 0) {
                ServerLog.e("No UID for " + packageName);
                return false;
            }
            IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService(Context.APP_OPS_SERVICE));
            appOpsService.setMode(op, packageUid, packageName, mode);
            if (!BuildConfig.RELEASE) {
                ServerLog.d("Set " + packageName + "'s " + AppOpsManager.opToName(op) + " to " + (
                        mode == AppOpsManager.MODE_ALLOWED ? "allow" : "ignore"));
            }
            return true;
        } catch (RemoteException e) {
            throw new HideApiException("Can't set run in background to " + mode + " for " + packageName, e);
        }
    }

    /**
     * since api-24
     */
    public static boolean setAllowBackground(String packageName, boolean allow) throws HideApiException {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                setMode(packageName, AppOpsManager.OP_RUN_IN_BACKGROUND, allow ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED);
    }

    private static Collection<String> getPackagesForOp(int op, int mode) {
        try {
            IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService(Context.APP_OPS_SERVICE));
            List<AppOpsManager.PackageOps> ops = appOpsService.getPackagesForOps(new int[] {op});
            if (ops == null || ops.isEmpty()) {
                return Collections.emptyList();
            }
            Collection<String> packageNames = new ArrayList<>();
            for (AppOpsManager.PackageOps pkg : ops) {
                for (AppOpsManager.OpEntry entry : pkg.getOps()) {
                    if (entry.getMode() == mode) {
                        packageNames.add(pkg.getPackageName());
                        break;
                    }
                }
            }
            return packageNames;
        } catch (RemoteException e) {
            throw new HideApiException("Can't get packages for ops", e);
        }
    }

    public static Collection<String> getVpnPackages() {
        try {
            // FIXME
            return getPackagesForOp(AppOpsManager.OP_ACTIVATE_VPN, AppOpsManager.MODE_ALLOWED);
        } catch (SecurityException e) {
            return Collections.emptyList();
        }
    }

    // AppOpsService end


    // UsageStatsManager start

    /**
     * since api-23
     */
    public static void setInactive(String packageName, boolean inactive) throws HideApiException {
        // android.permission.CHANGE_APP_IDLE_STATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                IUsageStatsManager usm = IUsageStatsManager.Stub.asInterface(ServiceManager.getService(Context.USAGE_STATS_SERVICE));
                usm.setAppInactive(packageName, inactive, USER_OWNER);
                if (!BuildConfig.RELEASE) {
                    ServerLog.d("Set " + packageName + " to " + (inactive ? "inactive" : "active"));
                }
            } catch (RemoteException e) {
                throw new HideApiException("Can't set " + packageName + " to " + (inactive ? "inactive" : "active"), e);
            }
        }
    }

    /**
     * since api-23
     */
    public static boolean getInactive(String packageName) throws HideApiException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                IUsageStatsManager usm = IUsageStatsManager.Stub.asInterface(ServiceManager.getService(Context.USAGE_STATS_SERVICE));
                return usm.isAppInactive(packageName, USER_OWNER);
            } catch (RemoteException e) {
                throw new HideApiException("Can't get inactive for " + packageName, e);
            }
        } else {
            return false;
        }
    }

    // UsageStatsManager end

    public static List<TaskRecord> getRunningActivities(File directory) throws HideApiException {
        try {
            IBinder am = ServiceManager.getService(Context.ACTIVITY_SERVICE);
            File file = File.createTempFile("dumpsys", "log", directory);
            ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
            am.dump(parcel.getFileDescriptor(), new String[] {"activities"});
            parcel.close();
            try {
                return parseActivities(file);
            } finally {
                file.delete();
            }
        } catch (IOException e) {
            ServerLog.e("Can't open file", e);
        } catch (RemoteException e) {
            throw new HideApiException("Can't dump activities", e);
        }
        return Collections.emptyList();
    }

    public static SimpleArrayMap<String, Set<String>> getDependencies(File directory) throws HideApiException {
        try {
            IBinder am = ServiceManager.getService(Context.ACTIVITY_SERVICE);
            File file = File.createTempFile("dumpsys", "log", directory);
            ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
            am.dump(parcel.getFileDescriptor(), new String[] {"processes"});
            parcel.close();
            try {
                return parseDependencies(file);
            } finally {
                file.delete();
            }
        } catch (IOException e) {
            ServerLog.e("Can't open file", e);
        } catch (RemoteException e) {
            throw new HideApiException("Can't dump activities", e);
        }
        return new SimpleArrayMap<>();
    }

    private static SimpleArrayMap<String, Set<String>> parseDependencies(File file) throws  IOException {
        SimpleArrayMap<String, Set<String>> dependencies = new SimpleArrayMap<>();
        try (
                BufferedReader reader = new BufferedReader(new FileReader(file))

        ) {
            Collection<String> packageList = null;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains("packageList=")) {
                    packageList = parseList(line);
                } else if (packageList != null && line.contains("packageDependencies=")) {
                    Collection<String> packageDependencies = parseList(line);
                    // remove self dependency
                    packageDependencies.removeAll(packageList);
                    for (String packageName : packageDependencies) {
                        int index = dependencies.indexOfKey(packageName);
                        if (index >= 0) {
                            dependencies.valueAt(index).addAll(packageList);
                        } else {
                            dependencies.put(packageName, new ArraySet<>(packageList));
                        }
                    }
                    packageList = null;
                }
            }
        }
        return dependencies;
    }

    private static Collection<String> parseList(String line) {
        // xx={yy, zz}
        int index = line.indexOf('=');
        Set<String> packageNames = new ArraySet<>();
        for (String packageName : line.substring(index + 0x2, line.length() - 0x1).split(",")) {
            packageNames.add(packageName.trim());
        }
        return packageNames;
    }

    private static List<TaskRecord> parseActivities(File file) throws IOException {
        List<TaskRecord> taskRecords = new ArrayList<>();
        TaskRecord taskRecord = null;
        String line;

        int stackId = -1;
        boolean top = false;
        boolean fullscreen = false;
        try (
                BufferedReader reader = new BufferedReader(new FileReader(file))
        ) {
            while ((line = reader.readLine()) != null) {
                // Stack #X:
                if (line.contains("Stack #")) {
                    stackId = parseStackId(line);
                    top = true;
                    fullscreen = false;
                } else if (line.contains("mFullscreen=")) {
                    fullscreen = Boolean.parseBoolean(StringUtils.substring(line, "mFullscreen=", ""));
                } else if (line.contains("mTaskToReturnTo=")) {
                    String returnTo = StringUtils.substring(line, "mTaskToReturnTo=", " ");
                    if (!"0".equals(returnTo)) {
                        top = false;
                    }
                } else if (line.contains("* TaskRecord{")) {
                    taskRecord = new TaskRecord();
                    taskRecord.stack = stackId;
                    taskRecord.fullscreen = fullscreen;
                    taskRecord.top = top;
                } else if (taskRecord != null) {
                    parseTaskRecord(taskRecord, line);
                    if (!TextUtils.isEmpty(taskRecord.state)) {
                        if (isDestroyed(taskRecord.state)) {
                            taskRecord.inactive = 0;
                        }
                        taskRecords.add(taskRecord);
                        taskRecord = null;
                    }
                }
            }
            return taskRecords;
        }
    }

    private static int parseStackId(String line) {
        String stack = StringUtils.substring(line, "Stack #", ":");
        if (StringUtils.isDigitsOnly(stack)) {
            return Integer.parseInt(stack);
        } else {
            ServerLog.w("Can't parse stack: " + line);
            return -1;
        }
    }

    private static void parseTaskRecord(TaskRecord tr, String line) {
        if (line.contains("userId=")) {
            String userId = StringUtils.substring(line, "userId=", " ");
            if (StringUtils.isDigitsOnly(userId)) {
                tr.userId = Integer.parseInt(userId);
            }
        } else if (line.contains(" cmp=")) {
            String packageName = StringUtils.substring(line, " cmp=", "/");
            if (!TextUtils.isEmpty(packageName)) {
                tr.packageName = packageName;
            }
        } else if (line.contains("inactive for ")) {
            String inactive = StringUtils.substring(line, "inactive for ", "s");
            if (!TextUtils.isEmpty(inactive) && TextUtils.isDigitsOnly(inactive)) {
                tr.inactive = Integer.parseInt(inactive);
            }
        } else if (line.contains(" state=")) {
            tr.state = StringUtils.substring(line, " state=", " ");
        }
    }

    private static boolean isDestroyed(String state) {
        return "DESTROYED".equals(state);
    }

    public static int getSystemPid() {
        for (ActivityManager.RunningAppProcessInfo process : HideApi.getRunningAppProcesses()) {
            for (String packageName : process.pkgList) {
                if ("android".equals(packageName)) {
                    return process.pid;
                }
            }
        }
        return 0;
    }

    // batterystats start
    public static boolean isCharging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                IBatteryStats batteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
                return batteryStats.isCharging();
            } catch (RemoteException e) {
                ServerLog.e("Can't get battery status", e);
                return false;
            }
        } else {
            return false;
        }
    }

    // batterystats end

    public static int getProcessState(ActivityManager.RunningAppProcessInfo process) {
        return process.processState;
    }

    public static ComponentName getRealActivity(ActivityManager.RecentTaskInfo recentTask) {
        return recentTask.realActivity;
    }

    public static final class PendingResult {

        private final String mResultData;

        PendingResult(String resultData) {
            this.mResultData = resultData;
        }

        public final String getResultData() {
            return mResultData;
        }

    }

    private static class IntentReceiver extends IIntentReceiver.Stub {

        private final CountDownLatch mLatch;

        private PendingResult mPendingResult;

        public IntentReceiver(CountDownLatch latch) {
            mLatch = latch;
        }

        @Override
        public void performReceive(Intent intent, int resultCode, String data, Bundle extras,
                                   boolean ordered, boolean sticky, int sendingUser) {
            // currently, we don't receive data via broadcast
            mPendingResult = new PendingResult(data);
            mLatch.countDown();
        }

        public PendingResult getPendingResult() {
            return mPendingResult;
        }
    }

    public static class HideApiException extends RuntimeException {

        public HideApiException(String s) {
            super(s);
        }

        public HideApiException(String s, Exception e) {
            super(s);
            ServerLog.e(s, e);
        }

    }

}
