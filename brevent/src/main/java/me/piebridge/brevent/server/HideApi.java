package me.piebridge.brevent.server;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.usage.IUsageStatsManager;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
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
import android.util.Log;

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

import me.piebridge.brevent.protocol.BreventIntent;

/**
 * Created by thom on 2016/12/28.
 */
class HideApi {

    static final int USER_OWNER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? HideApiOverrideN.USER_SYSTEM : HideApiOverride.USER_OWNER;

    private HideApi() {

    }

    // ActivityManager start

    public static void forceStopPackage(String packageName, String reason, int uid) throws HideApiException {
        try {
            ActivityManagerNative.getDefault().forceStopPackage(packageName, uid);
            ServerLog.d("Force stop " + packageName + reason);
        } catch (RemoteException e) {
            throw new HideApiException("Can't force stop " + packageName, e);
        }
    }

    public static int getCurrentUser() {
        try {
            return HideApiOverride.getCurrentUser();
        } catch (RuntimeException e) {
            ServerLog.w("Can't getCurrentUser");
            return HideApi.USER_OWNER;
        }
    }

    public static List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses() {
        try {
            return ActivityManagerNative.getDefault().getRunningAppProcesses();
        } catch (RemoteException e) {
            throw new HideApiException("Can't get running app processes", e);
        }
    }

    public static PendingResult sendBroadcast(Intent intent, int uid) throws HideApiException {
        try {
            CountDownLatch latch = new CountDownLatch(0x1);
            IntentReceiver receiver = new IntentReceiver(latch);
            String[] requiredPermissions = new String[] {BreventIntent.PERMISSION_MANAGER};
            ActivityManagerNative.getDefault().broadcastIntent(null, intent, null, receiver,
                    0, null, null, requiredPermissions,
                    HideApiOverride.OP_NONE, null, true, false, uid);
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
            return getPackageManager().getInstalledPackages(0, uid).getList();
        } catch (RemoteException e) {
            throw new HideApiException("Can't getInstalledPackages", e);
        }
    }

    public static List<PackageInfo> getGcmPackages(int uid) {
        try {
            ParceledListSlice<PackageInfo> result = getPackageManager().getPackagesHoldingPermissions(new String[] {
                    "com.google.android.c2dm.permission.RECEIVE",
            }, 0, uid);
            if (result != null) {
                return result.getList();
            } else {
                return Collections.emptyList();
            }
        } catch (RemoteException | SecurityException e) {
            ServerLog.d("Can't getGcmPackages", e);
            return Collections.emptyList();
        }
    }

    public static boolean isGcm(String packageName, int uid) {
        return hasGcmPermission(packageName, uid) && hasGcmReceiver(packageName, uid);
    }

    public static boolean hasGcmPermission(String packageName, int uid) {
        PackageInfo packageInfo = getPackageInfo(packageName, PackageManager.GET_PERMISSIONS, uid);
        if (packageInfo != null && packageInfo.permissions != null) {
            if (packageInfo.requestedPermissions == null) {
                return false;
            }
            int size = Math.min(packageInfo.requestedPermissionsFlags.length, packageInfo.requestedPermissions.length);
            for (int i = 0; i < size; ++i) {
                if ("com.google.android.c2dm.permission.RECEIVE".equals(packageInfo.requestedPermissions[i])) {
                    return (PackageInfo.REQUESTED_PERMISSION_GRANTED & packageInfo.requestedPermissionsFlags[i])
                            == PackageInfo.REQUESTED_PERMISSION_GRANTED;
                }
            }
        }
        return false;
    }

    public static boolean hasGcmReceiver(String packageName, int uid) {
        try {
            Intent intent = new Intent("com.google.android.c2dm.intent.RECEIVE");
            intent.setPackage(packageName);
            List receivers;
            IPackageManager packageManager = getPackageManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ParceledListSlice result = packageManager.queryIntentReceivers(intent, null, 0, uid);
                if (result != null) {
                    receivers = result.getList();
                } else {
                    receivers = null;
                }
            } else {
                receivers = HideApiOverrideM.queryIntentReceivers(packageManager, intent, uid);
            }
            return receivers != null && !receivers.isEmpty();
        } catch (RemoteException | SecurityException e) {
            ServerLog.d("Can't check GcmReceiver for " + packageName, e);
            return false;
        }
    }

    public static boolean isPackageAvailable(String packageName, int uid) throws HideApiException {
        try {
            return getPackageManager().isPackageAvailable(packageName, uid);
        } catch (RemoteException e) {
            throw new HideApiException("Can't isPackageAvailable for " + packageName, e);
        }
    }

    public static String getLauncher(int uid) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo resolveInfo = getPackageManager().resolveIntent(intent,
                    null, PackageManager.MATCH_DEFAULT_ONLY, uid);
            return resolveInfo.activityInfo.packageName;
        } catch (RemoteException e) {
            throw new HideApiException("Can't getLauncher", e);
        }
    }

    private static int getPackageUid(String packageName, int uid) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return getPackageManager().getPackageUid(packageName,
                        PackageManager.MATCH_UNINSTALLED_PACKAGES, uid);
            } else {
                return getPackageUidDeprecated(packageName, uid);
            }
        } catch (RemoteException e) {
            throw new HideApiException("Can't getPackageUid for " + packageName, e);
        }
    }

    @SuppressWarnings("deprecation")
    private static int getPackageUidDeprecated(String packageName, int uid) throws RemoteException {
        return getPackageManager().getPackageUid(packageName, uid);
    }

    public static PackageInfo getPackageInfo(String packageName, int flags, int uid) {
        try {
            return getPackageManager().getPackageInfo(packageName, flags, uid);
        } catch (RemoteException e) {
            throw new HideApiException("Can't getPackageInfo", e);
        }
    }

    public static void setStopped(String packageName, boolean stopped, int uid) {
        try {
            getPackageManager().setPackageStoppedState(packageName, stopped, uid);
        } catch (SecurityException | RemoteException e) {
            ServerLog.d("Can't setStopped for " + packageName + "(ignore)", e);
        }
    }

    // PackageManager end


    // UsageStatsManager start

    /**
     * since api-23
     */
    public static void setAppInactive(String packageName, boolean inactive, int uid) throws HideApiException {
        // android.permission.CHANGE_APP_IDLE_STATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                IUsageStatsManager usm = IUsageStatsManager.Stub.asInterface(ServiceManager.getService(Context.USAGE_STATS_SERVICE));
                usm.setAppInactive(packageName, inactive, uid);
                if (Log.isLoggable(ServerLog.TAG, Log.VERBOSE)) {
                    ServerLog.v("Set " + packageName + " to " + (inactive ? "inactive" : "active"));
                }
            } catch (RemoteException | SecurityException e) {
                throw new HideApiException("Can't set " + packageName + " to " + (inactive ? "inactive" : "active"), e);
            }
        }
    }

    /**
     * since api-23
     */
    public static boolean getAppInactive(String packageName, int uid) throws HideApiException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                IUsageStatsManager usm = IUsageStatsManager.Stub.asInterface(ServiceManager.getService(Context.USAGE_STATS_SERVICE));
                return usm.isAppInactive(packageName, uid);
            } catch (RemoteException | SecurityException e) {
                throw new HideApiException("Can't get inactive for " + packageName, e);
            }
        } else {
            return false;
        }
    }

    // UsageStatsManager end

    public static List<TaskRecord> getRunningActivities(File directory, int userId) throws HideApiException {
        try {
            IBinder am = ServiceManager.getService(Context.ACTIVITY_SERVICE);
            File file = File.createTempFile("dumpsys", "log", directory);
            ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
            am.dump(parcel.getFileDescriptor(), new String[] {"activities"});
            parcel.close();
            return parseActivities(file, userId);
        } catch (IOException e) {
            ServerLog.e("Can't open file", e);
        } catch (RemoteException e) {
            throw new HideApiException("Can't dump activities", e);
        }
        return Collections.emptyList();
    }

    public static Set<String> dumpWidgets(File directory, int userId, String launcher) {
        if (launcher == null) {
            ServerLog.e("Can't dump widgets, no launcher");
            return Collections.emptySet();
        }
        try {
            IBinder am = ServiceManager.getService(Context.NOTIFICATION_SERVICE);
            File file = File.createTempFile("dumpsys", "appwidget", directory);
            ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
            am.dump(parcel.getFileDescriptor(), new String[0]);
            parcel.close();
            return parseWidgets(file, userId, launcher);
        } catch (IOException e) {
            ServerLog.e("Can't open file", e);
        } catch (RemoteException | SecurityException e) {
            ServerLog.d("Can't dump widgets (ignore)", e);
        }
        return Collections.emptySet();
    }

    private static Set<String> parseWidgets(File file, int userId, String launcher) throws IOException {
        Set<String> packageNames = new ArraySet<>();
        try (
                BufferedReader reader = new BufferedReader(new FileReader(file))

        ) {
            String line;
            boolean start = false;
            String pkg = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Widgets:")) {
                    start = true;
                } else if (start) {
                    if (line.startsWith("Hosts:")) {
                        break;
                    }
                    if (line.contains("HostId")) {
                        String user = StringUtils.substring(line, "user:", ",");
                        if (StringUtils.isDigitsOnly(user) && Integer.parseInt(user) == userId) {
                            pkg = StringUtils.substring(line, "pkg:", "}");
                        } else {
                            pkg = null;
                        }
                    } else if (launcher.equals(pkg) && line.contains("ProviderId")) {
                        String user = StringUtils.substring(line, "user:", ",");
                        if (StringUtils.isDigitsOnly(user) && Integer.parseInt(user) == userId) {
                            packageNames.add(StringUtils.substring(line, "ComponentInfo{", "/"));
                        }
                    }
                }
            }
        }
        if (!file.delete()) {
            ServerLog.d("Can't delete file: " + file);
        }
        return packageNames;
    }

    public static SimpleArrayMap<String, Boolean> dumpNotifications(File directory, int userId) {
        try {
            IBinder am = ServiceManager.getService(Context.NOTIFICATION_SERVICE);
            File file = File.createTempFile("dumpsys", "log", directory);
            ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
            am.dump(parcel.getFileDescriptor(), new String[0]);
            parcel.close();
            return parseNotifications(file, userId);
        } catch (IOException e) {
            ServerLog.e("Can't open file", e);
        } catch (RemoteException | SecurityException e) {
            ServerLog.d("Can't dump notifications", e);
        }
        return new SimpleArrayMap<>();
    }

    private static SimpleArrayMap<String, Boolean> parseNotifications(File file, int userId) throws IOException {
        SimpleArrayMap<String, Boolean> notifications = new SimpleArrayMap<>();
        Set<String> packageNames = new ArraySet<>();
        try (
                BufferedReader reader = new BufferedReader(new FileReader(file))

        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("NotificationRecord")) {
                    String packageName = StringUtils.substring(line, "pkg=", " ");
                    String userHandle = StringUtils.substring(line, "UserHandle{", "}");
                    if (StringUtils.isDigitsOnly(userHandle) && Integer.parseInt(userHandle) == userId && !TextUtils.isEmpty(packageName)) {
                        packageNames.add(packageName);
                    }
                } else if (line.contains("priority=MAX")) {
                    String packageName = StringUtils.substring(line.trim(), "", " ");
                    notifications.put(packageName, false);
                }
            }
        }
        for (String packageName : packageNames) {
            int indexOfKey = notifications.indexOfKey(packageName);
            if (indexOfKey >= 0) {
                notifications.setValueAt(indexOfKey, true);
            } else {
                notifications.put(packageName, null);
            }
        }
        if (!file.delete()) {
            ServerLog.d("Can't delete file: " + file);
        }
        return notifications;
    }

    public static SimpleArrayMap<String, Set<String>> getDependencies(File directory, int userId) throws HideApiException {
        try {
            IBinder am = ServiceManager.getService(Context.ACTIVITY_SERVICE);
            File file = File.createTempFile("dumpsys", "log", directory);
            ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
            am.dump(parcel.getFileDescriptor(), new String[] {"processes"});
            parcel.close();
            return parseDependencies(file, userId);
        } catch (IOException e) {
            ServerLog.e("Can't open file", e);
        } catch (RemoteException | SecurityException e) {
            ServerLog.d("Can't dump dependencies", e);
        }
        return new SimpleArrayMap<>();
    }

    private static SimpleArrayMap<String, Set<String>> parseDependencies(File file, int userId) throws IOException {
        SimpleArrayMap<String, Set<String>> dependencies = new SimpleArrayMap<>();
        try (
                BufferedReader reader = new BufferedReader(new FileReader(file))

        ) {
            Integer user = null;
            Collection<String> packageList = null;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains("user #")) {
                    user = Integer.parseInt(StringUtils.substring(line, "user #", " "));
                } else if ((user == null || user == userId) && line.contains("packageList=")) {
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
                    user = null;
                }
            }
        }
        if (!file.delete()) {
            ServerLog.d("Can't delete file: " + file);
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

    private static List<TaskRecord> parseActivities(File file, int userId) throws IOException {
        List<TaskRecord> taskRecords = new ArrayList<>();
        TaskRecord taskRecord = null;
        String line;

        int stackId = -1;
        boolean top = false;
        try (
                BufferedReader reader = new BufferedReader(new FileReader(file))
        ) {
            while ((line = reader.readLine()) != null) {
                if (line.contains("mFocusedActivity:")) {
                    break;
                }
                // Stack #X:
                if (line.contains("Stack #")) {
                    top = true;
                    stackId = parseStackId(line);
                } else if (line.contains("* TaskRecord{")) {
                    taskRecord = new TaskRecord();
                    taskRecord.top = top;
                    taskRecord.stack = stackId;
                } else if (taskRecord != null) {
                    parseTaskRecord(taskRecord, line);
                    if (!TextUtils.isEmpty(taskRecord.state)) {
                        addTaskRecordsIfNeeded(taskRecords, taskRecord, userId);
                        top = false;
                        taskRecord = null;
                    }
                }
            }
            if (taskRecords.isEmpty()) {
                ServerLog.d("Can't parse task records from file: " + file);
            } else if (!file.delete()) {
                ServerLog.d("Can't delete file: " + file);
            }
            return taskRecords;
        }
    }

    private static void addTaskRecordsIfNeeded(List<TaskRecord> taskRecords, TaskRecord taskRecord, int userId) {
        if (isDestroyed(taskRecord.state)) {
            taskRecord.inactive = 0;
        }
        if (taskRecord.userId == null || taskRecord.userId == userId) {
            taskRecords.add(taskRecord);
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

    // batterystats start
    public static boolean isCharging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                IBatteryStats batteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
                return batteryStats.isCharging();
            } catch (RemoteException | SecurityException e) {
                ServerLog.d("Can't get battery status", e);
            }
        }
        return false;
    }

    public static IPackageManager getPackageManager() {
        IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (packageManager == null) {
            ServerLog.e("Could not access the Package Manager. Is the system running?");
            System.exit(1);
        }
        return packageManager;
    }

    // batterystats end

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
