package me.piebridge.brevent.ui;

import android.content.pm.PackageManager;

import me.piebridge.brevent.protocol.BreventPackageInfo;

/**
 * Created by thom on 2017/1/25.
 */
public class FrameworkAppsFragment extends AppsFragment {

    @Override
    public boolean accept(PackageManager packageManager, BreventPackageInfo packageInfo) {
        return isFrameworkPackage(packageManager, packageInfo);
    }

}
