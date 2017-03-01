package me.piebridge.brevent.loader;

import android.app.AppGlobals;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Process;
import android.os.RemoteException;
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
import me.piebridge.brevent.server.HideApiOverride;
import me.piebridge.brevent.server.HideApiOverrideN;

/**
 * Brevent Shell entry
 * <p>
 * Created by thom on 2016/11/22.
 */
public class Brevent implements Runnable {

    private static final String TAG = "BreventLoader";

    private static final int BUFFER = 0x2000;

    private static final String BREVENT_PACKAGE = "me.piebridge.brevent";

    private static final String BREVENT_CLASS = "me.piebridge.brevent.server.BreventServer";

    private static final String LIB_READER = "lib" + "reader" + ".so";

    private static final String LIB_LOADER = "lib" + "loader" + ".so";

    private static final int USER_OWNER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? HideApiOverrideN.USER_SYSTEM : HideApiOverride.USER_OWNER;

    private static final int MINUTE_SURVIVE_TIME = 30;

    private final Method mMain;

    private final CountDownLatch mLatch;

    public Brevent(Method main, CountDownLatch latch) {
        mMain = main;
        mLatch = latch;
    }

    @Override
    public void run() {
        try {
            mMain.invoke(null, (Object) null);
        } catch (ReflectiveOperationException e) {
            Log.d(TAG, "Can't run brevent server", e);
            throw new RuntimeException(e);
        } finally {
            mLatch.countDown();
        }
    }

    private static String getDataDir() throws RemoteException {
        int uid = HideApiOverride.uidForData(Process.myUid());
        String[] packageNames = getPackagesForUid(uid);
        if (packageNames != null) {
            for (String packageName : packageNames) {
                String dataDir = getPackageInfo(packageName, 0, USER_OWNER).applicationInfo.dataDir;
                if (dataDir != null) {
                    return dataDir;
                }
            }
        }
        String message = "Can't find package for " + uid;
        Log.e(TAG, message);
        throw new UnsupportedOperationException(message);
    }

    private static File copyFile(File from, File to, String name) throws IOException {
        if (!to.isDirectory() && !to.mkdirs()) {
            String message = "Can't make sure directory: " + to;
            Log.d(TAG, message);
            throw new UnsupportedOperationException(message);
        }
        File input = new File(from, name);
        File output = new File(to, name);
        try (
                InputStream is = new FileInputStream(input);
                OutputStream os = new FileOutputStream(output)
        ) {
            int length;
            byte[] bytes = new byte[BUFFER];
            while ((length = is.read(bytes, 0, BUFFER)) != -1) {
                os.write(bytes, 0, length);
            }
            return output;
        }
    }

    private static String[] getPackagesForUid(int uid) throws RemoteException {
        return AppGlobals.getPackageManager().getPackagesForUid(uid);
    }

    private static PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException {
        return AppGlobals.getPackageManager().getPackageInfo(packageName, flags, userId);
    }

    public static void main(String[] args) throws Exception {
        PackageInfo packageInfo = getPackageInfo(BREVENT_PACKAGE, 0, USER_OWNER);
        File nativeLibraryDir = new File(packageInfo.applicationInfo.nativeLibraryDir);
        File libDir = new File(getDataDir(), "brevent");
        File libReader = copyFile(nativeLibraryDir, libDir, LIB_READER);
        File libLoader = copyFile(nativeLibraryDir, libDir, LIB_LOADER);
        Log.d(TAG, "lib: " + libDir + ", loader: " + libLoader);
        ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader().getParent();
        ClassLoader loadClassLoader = new PathClassLoader(libLoader.getAbsolutePath(), libDir.getAbsolutePath(), bootClassLoader);
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        long previous = System.currentTimeMillis();
        while (packageInfo != null) {
            ClassLoader classLoader = new PathClassLoader(packageInfo.applicationInfo.sourceDir, loadClassLoader);
            Method main = classLoader.loadClass(BREVENT_CLASS).getMethod("main", String[].class);
            CountDownLatch latch = new CountDownLatch(0x1);
            new Thread(new Brevent(main, latch)).start();
            latch.await();
            long now = System.currentTimeMillis();
            if (TimeUnit.MILLISECONDS.toSeconds(now - previous) < MINUTE_SURVIVE_TIME) {
                Log.d(TAG, "Brevent Server quit in " + MINUTE_SURVIVE_TIME + " seconds, quit");
                break;
            }
            previous = now;
            packageInfo = getPackageInfo(BREVENT_PACKAGE, 0, USER_OWNER);
        }
        if (packageInfo == null) {
            if (!libLoader.delete() || !libReader.delete() || !libDir.delete()) {
                Log.d(TAG, "Can't remove brevent loader");
            }
        }
    }

}
