package me.piebridge.brevent.ui;

import android.accounts.NetworkErrorException;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.WorkerThread;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.InviteEvent;
import com.crashlytics.android.answers.LoginEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dalvik.system.PathClassLoader;
import io.fabric.sdk.android.Fabric;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.override.HideApiOverride;
import me.piebridge.brevent.override.HideApiOverrideN;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.brevent.protocol.BreventProtocol;
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

    public static final boolean IS_OWNER = HideApiOverride.getUserId() == getOwner();

    private boolean fetched;

    private WeakReference<Handler> handlerReference;

    private boolean eventMade;

    private static ExecutorService executor = new ScheduledThreadPoolExecutor(0x1);

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
            String mode = getMode();
            UILog.d("days: " + days + ", living: " + living);
            if ("root".equals(mode)) {
                Answers.getInstance().logInvite(new InviteEvent()
                        .putMethod(mode)
                        .putCustomAttribute("standby", Boolean.toString(mSupportStandby))
                        .putCustomAttribute("stopped", Boolean.toString(mSupportStopped))
                        .putCustomAttribute("days", Long.toString(days))
                        .putCustomAttribute("living", Long.toString(living))
                        .putCustomAttribute("installer", getInstaller()));
            } else {
                Answers.getInstance().logLogin(new LoginEvent()
                        .putMethod(mode).putSuccess(true)
                        .putCustomAttribute("standby", Boolean.toString(mSupportStandby))
                        .putCustomAttribute("stopped", Boolean.toString(mSupportStopped))
                        .putCustomAttribute("days", Long.toString(days))
                        .putCustomAttribute("living", Long.toString(living))
                        .putCustomAttribute("installer", getInstaller()));
            }
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

    public void notifyRootCompleted(List<String> output) {
        if (handlerReference != null) {
            Handler handler = handlerReference.get();
            if (handler != null) {
                Message message = handler.obtainMessage(BreventActivity.MESSAGE_ROOT_COMPLETED,
                        output);
                handler.sendMessageDelayed(message, 0x400);
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

    public static boolean checkPort() throws NetworkErrorException {
        Future<Boolean> future = executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return checkPortSync();
            }
        });
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            UILog.d("Can't check port: " + e.getMessage(), e);
            throw new NetworkErrorException(e);
        } finally {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    @WorkerThread
    public static boolean checkPortSync() {
        try (
                Socket socket = new Socket(InetAddress.getLoopbackAddress(), BreventProtocol.PORT);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                DataInputStream is = new DataInputStream(socket.getInputStream())
        ) {
            os.writeShort(0);
            os.flush();
            BreventProtocol.readFrom(is);
            UILog.v("connected to localhost: " + BreventProtocol.PORT);
            return true;
        } catch (ConnectException e) {
            UILog.v("cannot connect to localhost:" + BreventProtocol.PORT, e);
            return false;
        } catch (IOException e) {
            UILog.v("io error to localhost:" + BreventProtocol.PORT, e);
            return false;
        }
    }

    public boolean hasXposed() {
        String clazzServer = String.valueOf(BuildConfig.SERVER);
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        try {
            String sourceDir = getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0)
                    .sourceDir;
            PathClassLoader classLoader = new PathClassLoader(sourceDir, systemClassLoader);
            return (boolean) classLoader.loadClass(clazzServer).getMethod(String.valueOf('c'))
                    .invoke(null);
        } catch (ReflectiveOperationException | PackageManager.NameNotFoundException e) { // NOSONAR
            // do nothing
            return false;
        }
    }

}
