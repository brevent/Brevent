package me.piebridge.brevent.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Created by thom on 2017/1/25.
 */
public class SystemAppsFragment extends AppsFragment {

    @Override
    public boolean accept(PackageManager packageManager, PackageInfo packageInfo) {
        return BreventActivity.isSystemPackage(packageInfo.applicationInfo.flags)
                && !isFrameworkPackage(packageManager, packageInfo);
    }

}
