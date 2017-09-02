package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.Locale;

/**
 * Created by thom on 2017/1/31.
 */
public class AppsLabelLoader {

    private static final char[] WHITESPACE = ("\u0085\u00a0\u1680"
            + "\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a"
            + "\u2028\u2029\u202f\u205f\u3000").toCharArray();

    private static final String KEY_LAST_SYNC = "lastSync";

    private long mLastUpdateTime;

    private final long mLastSync;

    private final SharedPreferences mPreferences;

    private static long lastSync;

    public AppsLabelLoader(Context context) {
        mPreferences = context.getSharedPreferences("label-" + LocaleUtils.getSystemLocale(),
                Context.MODE_PRIVATE);
        lastSync = mLastSync = mPreferences.getLong(KEY_LAST_SYNC, 0);
    }

    public static long getLastSync(Context context) {
        if (lastSync == 0) {
            lastSync = context.getSharedPreferences("label-" + LocaleUtils.getSystemLocale(),
                    Context.MODE_PRIVATE).getLong(KEY_LAST_SYNC, 0);
        }
        return lastSync;
    }

    public String loadLabel(PackageManager packageManager, PackageInfo packageInfo) {
        long lastUpdateTime = packageInfo.lastUpdateTime;
        if (lastUpdateTime > mLastUpdateTime) {
            mLastUpdateTime = lastUpdateTime;
        }
        String packageName = packageInfo.packageName;
        if (lastUpdateTime <= mLastSync && mPreferences.contains(packageName)) {
            return mPreferences.getString(packageName, packageName);
        } else {
            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
            CharSequence label;
            if (launchIntent == null) {
                label = packageInfo.applicationInfo.loadLabel(packageManager);
            } else {
                label = packageManager.resolveActivity(launchIntent, 0).activityInfo
                        .loadLabel(packageManager);
            }
            if (label == null) {
                label = packageName;
            }
            String name = trim(label).toString();
            if (!label.equals(mPreferences.getString(packageName, null))) {
                mPreferences.edit().putString(packageName, name).apply();
            }
            return name;
        }
    }

    public void onCompleted() {
        if (mLastSync < mLastUpdateTime) {
            mPreferences.edit().putLong(KEY_LAST_SYNC, mLastUpdateTime).apply();
        }
    }

    private static boolean isWhiteSpace(char s) {
        if (s <= ' ') {
            return true;
        }
        for (char c : WHITESPACE) {
            if (c == s) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    static CharSequence trim(@NonNull CharSequence cs) {
        int last = cs.length() - 1;
        int start = 0;
        int end = last;
        while (start <= end && isWhiteSpace(cs.charAt(start))) {
            start++;
        }
        while (end >= start && isWhiteSpace(cs.charAt(end))) {
            --end;
        }
        if (start != 0 || end != last) {
            return cs.subSequence(start, end + 1);
        }
        return cs;
    }

}
