package me.piebridge.brevent.ui;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.RatingEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;
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

    long mDaemonTime;

    long mServerTime;

    int mUid;

    boolean started;

    public static final boolean IS_OWNER = HideApiOverride.getUserId() == getOwner();

    private boolean fetched;

    private WeakReference<Handler> handlerReference;

    private boolean eventMade;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.RELEASE) {
            Fabric.with(this, new Answers());
        }
    }

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
            String nativeLibraryDir = packageManager
                    .getApplicationInfo(BuildConfig.APPLICATION_ID, 0).nativeLibraryDir;
            File file = new File(nativeLibraryDir, "libbrevent.so");
            if (file.exists()) {
                return file;
            }
        } catch (PackageManager.NameNotFoundException e) {
            UILog.d("Can't find " + BuildConfig.APPLICATION_ID, e);
        }
        return null;
    }

    public String copyBrevent() {
        File brevent = getBootstrapFile();
        if (brevent == null) {
            return null;
        }
        File parent = getFilesDir().getParentFile();
        String father = parent.getParent();
        try {
            father = Os.readlink(father);
            parent = new File(father, parent.getName());
        } catch (ErrnoException e) {
            UILog.d("Can't read link for " + father, e);
        }
        File output = new File(parent, "brevent.sh");
        String path = output.getAbsolutePath();
        if (!copied) {
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
                }
                copied = true;
            } catch (IOException e) {
                UILog.d("Can't copy brevent", e);
                return null;
            }
        }
        try {
            Os.chmod(parent.getPath(), 00755);
            Os.chmod(path, 00755);
        } catch (ErrnoException e) {
            UILog.d("Can't chmod brevent", e);
            return null;
        }
        return path;
    }

    private void setSupportStandby(boolean supportStandby) {
        mSupportStandby = supportStandby;
    }

    public boolean supportStandby() {
        return mSupportStandby;
    }

    public String getInstaller() {
        String installer = getPackageManager().getInstallerPackageName(BuildConfig.APPLICATION_ID);
        if (TextUtils.isEmpty(installer)) {
            return "unknown";
        } else {
            return installer;
        }
    }

    public String getMode() {
        return HideApiOverride.isShell(mUid) ? "shell" :
                (HideApiOverride.isRoot(mUid) ? "root" : "unknown");
    }

    public void updateStatus(BreventResponse breventResponse) {
        boolean shouldUpdated = mDaemonTime != breventResponse.mDaemonTime;
        mDaemonTime = breventResponse.mDaemonTime;
        mServerTime = breventResponse.mServerTime;
        mUid = breventResponse.mUid;
        setSupportStandby(breventResponse.mSupportStandby);
        setSupportStopped(breventResponse.mSupportStopped);
        if (BuildConfig.RELEASE && shouldUpdated) {
            long days = TimeUnit.MILLISECONDS.toDays(mServerTime - mDaemonTime);
            long living = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - mDaemonTime);
            int rating = (int) (days / 7) + ((days % 7 == 0) ? 0 : 1);
            if (rating == 0) {
                rating = 1;
            }
            if (rating > 5) {
                rating = 5;
            }
            String mode = getMode();
            Answers.getInstance().logRating(new RatingEvent()
                    .putRating(rating)
                    .putContentName("Brevent")
                    .putContentType(mode)
                    .putContentId("brevent-" + mode)
                    .putCustomAttribute("standby", Boolean.toString(mSupportStandby))
                    .putCustomAttribute("stopped", Boolean.toString(mSupportStopped))
                    .putCustomAttribute("days", days)
                    .putCustomAttribute("living", living)
                    .putCustomAttribute("installer", getInstaller()));
            UILog.i("logRating");
        }
    }

    private static int getOwner() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? HideApiOverrideN.USER_SYSTEM
                : HideApiOverride.USER_OWNER;
    }

    public boolean isRunningAsRoot() {
        return handlerReference != null && handlerReference.get() != null;
    }

    public void setHandler(Handler handler) {
        if (handlerReference == null || handlerReference.get() != handler) {
            handlerReference = new WeakReference<>(handler);
        }
    }

    public void notifyRootCompleted() {
        if (handlerReference != null) {
            Handler handler = handlerReference.get();
            if (handler != null) {
                handler.sendEmptyMessage(BreventActivity.MESSAGE_ROOT_COMPLETED);
            }
            handlerReference = null;
        }
    }

    public boolean isEventMade() {
        return eventMade;
    }

    public void makeEvent() {
        if (!eventMade) {
            eventMade = true;
        }
    }

    public void resetEvent() {
        if (eventMade) {
            eventMade = false;
        }
    }

}
