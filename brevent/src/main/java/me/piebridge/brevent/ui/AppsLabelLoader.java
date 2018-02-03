package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.Objects;

import me.piebridge.SimpleTrim;

/**
 * Created by thom on 2017/1/31.
 */
public class AppsLabelLoader {

    private static final String KEY_LAST_SYNC = "lastSync";

    private long mLastUpdateTime;

    private final long mLastSync;

    private final SharedPreferences mPreferences;

    private static long lastSync;

    public AppsLabelLoader(Context context) {
        mPreferences = context.getSharedPreferences("label-" + LocaleUtils.getSystemLocale(),
                Context.MODE_PRIVATE);
        mLastSync = mPreferences.getLong(KEY_LAST_SYNC, 0);
    }

    public static long getLastSync(Context context) {
        if (lastSync == 0) {
            lastSync = new AppsLabelLoader(context).mLastSync;
        }
        return lastSync;
    }

    public String getLabel(PackageManager packageManager, PackageInfo packageInfo) {
        long lastUpdateTime = packageInfo.lastUpdateTime;
        if (lastUpdateTime > mLastUpdateTime) {
            mLastUpdateTime = lastUpdateTime;
        }
        String packageName = packageInfo.packageName;
        if (lastUpdateTime <= mLastSync && mPreferences.contains(packageName)) {
            return mPreferences.getString(packageName, packageName);
        } else {
            String name = loadLabel(packageManager, packageInfo);
            if (!Objects.equals(name, mPreferences.getString(packageName, null))) {
                mPreferences.edit().putString(packageName, name).apply();
            }
            return name;
        }
    }

    public static String loadLabel(PackageManager packageManager, PackageInfo packageInfo) {
        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageInfo.packageName);
        CharSequence label;
        if (launchIntent == null) {
            label = packageInfo.applicationInfo.loadLabel(packageManager);
        } else {
            label = packageManager.resolveActivity(launchIntent, 0)
                    .activityInfo.loadLabel(packageManager);
        }
        if (label == null) {
            label = packageInfo.packageName;
        }
        return SimpleTrim.trim(label).toString();
    }

    public void onCompleted() {
        if (mLastSync < mLastUpdateTime) {
            mPreferences.edit().putLong(KEY_LAST_SYNC, mLastUpdateTime).apply();
        }
    }

}
