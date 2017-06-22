package me.piebridge.brevent.ui;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.override.HideApiOverride;
import me.piebridge.brevent.override.HideApiOverrideN;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.brevent.protocol.BreventResponse;

/**
 * Created by thom on 2017/2/7.
 */
public class BreventApplication extends Application {

    private boolean allowRoot;

    private boolean mSupportStopped = true;

    private boolean mSupportStandby = false;

    private boolean copied;

    public long mDaemonTime;

    public long mServerTime;

    public int mUid;

    public boolean started;

    public static boolean IS_OWNER = HideApiOverride.getUserId() == getOwner();

    private boolean fetched;

    private WeakReference<Handler> handlerReference;

    public void toggleAllowRoot() {
        allowRoot = !allowRoot;
    }

    public synchronized boolean allowRoot() {
        if (!fetched) {
            fetched = true;
            allowRoot = PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, false);
        }
        return allowRoot;
    }

    private void setSupportStopped(boolean supportStopped) {
        if (mSupportStopped != supportStopped) {
            mSupportStopped = supportStopped;
        }
    }

    public boolean supportStopped() {
        return mSupportStopped;
    }

    private File getBootstrapFile() {
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo ai = packageManager.getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
            File file = new File(ai.nativeLibraryDir, "libbrevent.so");
            if (file.exists()) {
                return file;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        return null;
    }

    public String copyBrevent() {
        File file = getApplicationContext().getExternalFilesDir(null);
        File brevent = getBootstrapFile();
        if (file == null || brevent == null) {
            return null;
        }
        String sdcard = "/" + "sdcard";
        String path = buildPath(new File(sdcard),
                "Android", "data", BuildConfig.APPLICATION_ID, "brevent.sh").getAbsolutePath();
        if (!copied) {
            try {
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                File output = buildPath(externalStorageDirectory,
                        "Android", "data", BuildConfig.APPLICATION_ID, "brevent.sh");
                try (
                        InputStream is = getResources().openRawResource(R.raw.brevent);
                        OutputStream os = new FileOutputStream(output);
                        PrintWriter pw = new PrintWriter(os)
                ) {
                    pw.println("#!/system/bin/sh");
                    pw.println();
                    pw.println("path=" + brevent);
                    pw.println("abi64=" + brevent.getPath().contains("64"));
                    pw.println();
                    pw.flush();
                    byte[] bytes = new byte[0x400];
                    int length;
                    while ((length = is.read(bytes)) != -1) {
                        os.write(bytes, 0, length);
                        if (length < 0x400) {
                            break;
                        }
                    }
                }
                copied = true;
            } catch (IOException e) {
                UILog.d("Can't copy brevent", e);
                return null;
            }
        }
        return path;
    }

    private File buildPath(File base, String... children) {
        File path = base;
        for (String child : children) {
            if (child != null) {
                if (path == null) {
                    path = new File(child);
                } else {
                    path = new File(path, child);
                }
            }
        }
        return path;
    }

    private void setSupportStandby(boolean supportStandby) {
        mSupportStandby = supportStandby;
    }

    public boolean supportStandby() {
        return mSupportStandby;
    }

    public void updateStatus(BreventResponse breventResponse) {
        mDaemonTime = breventResponse.mDaemonTime;
        mServerTime = breventResponse.mServerTime;
        mUid = breventResponse.mUid;
        setSupportStandby(breventResponse.mSupportStandby);
        setSupportStopped(breventResponse.mSupportStopped);
    }

    private static int getOwner() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? HideApiOverrideN.USER_SYSTEM
                : HideApiOverride.USER_OWNER;
    }

    public void setHandler(Handler handler) {
        handlerReference = new WeakReference<>(handler);
    }

    public void notifyRootCompleted() {
        if (handlerReference != null) {
            Handler handler = handlerReference.get();
            if (handler != null) {
                handler.sendEmptyMessage(BreventActivity.MESSAGE_ROOT_COMPLETED);
            }
        }
    }

}
