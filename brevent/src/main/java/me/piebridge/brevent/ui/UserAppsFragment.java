package me.piebridge.brevent.ui;

import android.content.pm.ApplicationInfo;

/**
 * Created by thom on 2017/1/25.
 */
public class UserAppsFragment extends AppsFragment {

    @Override
    public boolean accept(ApplicationInfo applicationInfo) {
        return !isSystemPackage(applicationInfo.flags)
                && !isFrameworkPackage(applicationInfo.packageName);
    }

    @Override
    public boolean supportAllApps() {
        return true;
    }

}
