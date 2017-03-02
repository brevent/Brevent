package me.piebridge.brevent.ui;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import me.piebridge.brevent.BuildConfig;

public class BreventIntentService extends IntentService {

    public BreventIntentService() {
        super("BreventIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        startBrevent();
    }

    private void startBrevent() {
        String name = "libbootstrap.so";
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
            File file = new File(applicationInfo.nativeLibraryDir, name);
            if (file.exists()) {
                List<String> results = Shell.SU.run(file.getAbsolutePath());
                for (String result : results) {
                    UILog.d(result);
                }
            } else {
                UILog.e("Can't find libbootstrap.so");
            }
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
    }

    public static void startBrevent(Context context) {
        context.startService(new Intent(context, BreventIntentService.class));
    }

}
