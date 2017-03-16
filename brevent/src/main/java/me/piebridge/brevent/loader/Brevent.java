package me.piebridge.brevent.loader;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dalvik.system.PathClassLoader;
import me.piebridge.EventHandler;
import me.piebridge.brevent.server.HideApiOverride;
import me.piebridge.brevent.server.HideApiOverrideN;

/**
 * Brevent Shell entry
 * <p>
 * Created by thom on 2016/11/22.
 */
public class Brevent {

    private static final String TAG = "BreventLoader";

    private static final String BREVENT_PACKAGE = "me.piebridge.brevent";

    private static final String BREVENT_CLASS = "me.piebridge.brevent.server.BreventServer";

    private static final int USER_OWNER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? HideApiOverrideN.USER_SYSTEM : HideApiOverride.USER_OWNER;

    private static final int MIN_SURVIVE_TIME = 30;

    public static void main(String[] args) throws Exception {
        IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (packageManager == null) {
            Log.e(TAG, "Could not access the Package Manager. Is the system running?");
            System.exit(1);
        }
        PackageInfo packageInfo = packageManager.getPackageInfo(BREVENT_PACKAGE, 0, USER_OWNER);
        if (packageInfo == null) {
            Log.e(TAG, "Could not find " + BREVENT_PACKAGE);
            System.exit(1);
        }
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        Log.d(TAG, "classloader: " + EventHandler.class.getClassLoader());
        ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader().getParent();
        ClassLoader classLoader = new PathClassLoader(applicationInfo.sourceDir, applicationInfo.nativeLibraryDir, bootClassLoader);
        Method main = classLoader.loadClass(BREVENT_CLASS).getMethod("main", String[].class);
        long previous = System.currentTimeMillis();
        try {
            main.invoke(null, (Object) null);
        } catch (ReflectiveOperationException e) {
            Log.w(TAG, "Can't run brevent server", e);
            throw new RuntimeException(e);
        }
        long now = System.currentTimeMillis();
        if (TimeUnit.MILLISECONDS.toSeconds(now - previous) < MIN_SURVIVE_TIME) {
            Log.e(TAG, "Brevent Server quit in " + MIN_SURVIVE_TIME + " seconds, quit");
            System.exit(1);
        }
    }

}
