package me.piebridge.brevent.ui;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * Created by thom on 2017/1/25.
 */
public class FrameworkAppsFragment extends AppsFragment {

    @Override
    public boolean accept(ApplicationInfo applicationInfo) {
        return isFrameworkPackage(applicationInfo.packageName);
    }

    @Override
    public boolean isAllImportant() {
        return true;
    }

}
