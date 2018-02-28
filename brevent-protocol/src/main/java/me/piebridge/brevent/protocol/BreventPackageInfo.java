package me.piebridge.brevent.protocol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.util.IconDrawableFactory;

/**
 * Created by thom on 2018/2/28.
 */
public class BreventPackageInfo {

    public final String packageName;

    public final long firstInstallTime;

    public final long lastUpdateTime;

    public final int flags;

    public final int uid;

    public final int targetSdkVersion;

    public final ApplicationInfo applicationInfo;

    public final String launcherName;

    public BreventPackageInfo(PackageInfo instantPackage, String launcherName) {
        packageName = instantPackage.packageName;
        firstInstallTime = instantPackage.firstInstallTime;
        lastUpdateTime = instantPackage.lastUpdateTime;
        this.launcherName = launcherName;

        applicationInfo = instantPackage.applicationInfo;
        flags = applicationInfo.flags;
        uid = applicationInfo.uid;
        targetSdkVersion = applicationInfo.targetSdkVersion;
    }

    public BreventPackageInfo(Parcel in) {
        packageName = in.readString();
        firstInstallTime = in.readLong();
        lastUpdateTime = in.readLong();
        this.launcherName = in.readString();

        applicationInfo = new BreventApplicationInfo(in);
        flags = applicationInfo.flags;
        uid = applicationInfo.uid;
        targetSdkVersion = applicationInfo.targetSdkVersion;
    }

    public void writeToParcel(Parcel dest) {
        dest.writeString(packageName);
        dest.writeLong(firstInstallTime);
        dest.writeLong(lastUpdateTime);
        dest.writeString(launcherName);
        new BreventApplicationInfo(applicationInfo).writeToParcel(dest);
    }

    public CharSequence loadLabel(PackageManager packageManager) {
        return applicationInfo.loadLabel(packageManager);
    }

    public Drawable loadIcon(Context context) {
        return IconDrawableFactory.newInstance(context).getBadgedIcon(applicationInfo);
    }

    @SuppressLint("WrongConstant")
    public void startActivity(Context context) {
        if (launcherName != null) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(packageName);
            intent.setClassName(packageName, launcherName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

}
