package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.donation.DonateActivity;

/**
 * Created by thom on 2017/3/2.
 */
public class VersionPreference extends Preference {

    public VersionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        Context context = getContext();
        Resources resources = context.getResources();
        String supported = resources.getString(R.string.brevent_about_version_supported);
        String unsupported = resources.getString(R.string.brevent_about_version_unsupported);
        BreventApplication application = (BreventApplication) context.getApplicationContext();
        String extra = resources.getString(R.string.brevent_about_version_extra,
                application.supportStandby() ? supported : unsupported,
                application.supportStopped() ? supported : unsupported,
                application.supportAppops() ? supported : unsupported);
        if (!BuildConfig.RELEASE) {
            return resources.getString(R.string.brevent_about_version_summary_debug,
                    BuildConfig.VERSION_NAME) + extra;
        } else {
            return resources.getString(R.string.brevent_about_version_summary,
                    BuildConfig.VERSION_NAME, getVersion(context)) + extra;
        }
    }

    static String getVersion(Context context) {
        PackageManager packageManager = context.getPackageManager();
        String installer = packageManager.getInstallerPackageName(BuildConfig.APPLICATION_ID);

        if (!TextUtils.isEmpty(installer)
                && packageManager.getLaunchIntentForPackage(installer) != null) {
            if ("com.meizu.mstore".equals(installer)) {
                return context.getString(R.string.brevent_about_version_meizu);
            } else if ("com.smartisanos.appstore".equals(installer)) {
                return context.getString(R.string.brevent_about_version_smartisan);
            } else if (DonateActivity.PACKAGE_PLAY.equals(installer)) {
                return context.getString(R.string.brevent_about_version_play);
            }
        }

        return context.getString(R.string.brevent_about_version_like_play);
    }

    static boolean isPlay(Context context) {
        PackageManager packageManager = context.getPackageManager();
        String installer = packageManager.getInstallerPackageName(BuildConfig.APPLICATION_ID);

        if (!TextUtils.isEmpty(installer) &&
                packageManager.getLaunchIntentForPackage(installer) != null) {
            UILog.d("installer: " + installer);
            if ("com.meizu.mstore".equals(installer) ||
                    "com.smartisanos.appstore".equals(installer)) {
                return false;
            }
        }

        return true;
    }

}
