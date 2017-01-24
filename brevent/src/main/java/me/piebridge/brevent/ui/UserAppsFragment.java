package me.piebridge.brevent.ui;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * Created by thom on 2017/1/25.
 */
public class UserAppsFragment extends AppsFragment {

    @Override
    public boolean accept(PackageManager packageManager, ApplicationInfo applicationInfo) {
        return !isSystemPackage(applicationInfo.flags)
                && !isFrameworkPackage(packageManager, applicationInfo.packageName);
    }

    @Override
    public boolean supportAllApps() {
        return true;
    }

}
