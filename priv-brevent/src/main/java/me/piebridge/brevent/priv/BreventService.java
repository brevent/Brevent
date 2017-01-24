package me.piebridge.brevent.priv;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import dalvik.system.DexClassLoader;

/**
 * Created by thom on 2017/1/18.
 */
public class BreventService extends Service implements Runnable {

    private static final String CLASS_NAME = "me.piebridge.brevent.server.Brevent";

    public static final String PACKAGE_NAME = "me.piebridge.brevent";

    public static final String TAG = "PrivBrevent";

    private String mSourceDir;

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(0x2);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        PackageInfo packageInfo;
        try {
            if (!checkPermission()) {
                return START_NOT_STICKY;
            }
            packageInfo = getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Can't find " + PACKAGE_NAME);
            return START_NOT_STICKY;
        }
        mSourceDir = packageInfo.applicationInfo.sourceDir;
        executor.submit(this);
        return START_STICKY;
    }

    private boolean checkPermission() throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
        int[] requestedPermissionsFlags = packageInfo.requestedPermissionsFlags;
        String[] requestedPermissions = packageInfo.requestedPermissions;
        Log.d(TAG, "requested permission: " + Arrays.toString(requestedPermissions));
        Log.d(TAG, "requested permission flags: " + Arrays.toString(requestedPermissionsFlags));
        int size = Math.min(requestedPermissions.length, requestedPermissionsFlags.length);
        for (int i = 0; i < size; ++i) {
            if ((requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != PackageInfo.REQUESTED_PERMISSION_GRANTED) {
                notifyPermission(requestedPermissions[i]);
                return false;
            }
        }
        return true;
    }

    private void notifyPermission(String requestedPermission) {
        Log.e(TAG, requestedPermission + " is not granted");
    }

    private void loadBrevent(String dexPath) {
        ClassLoader classLoader = new DexClassLoader(dexPath, getCodeCacheDir().getAbsolutePath(),
                null, Thread.currentThread().getContextClassLoader());
        try {
            Class<?> brevent = classLoader.loadClass(CLASS_NAME);
            Method main = brevent.getMethod("main", String[].class);
            main.invoke(null, (Object) null);
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, "Can't call " + CLASS_NAME, e);
        }
    }

    @Override
    public void run() {
        loadBrevent(mSourceDir);
    }

}
