package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.override.HideApiOverride;
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
        BreventApplication application = (BreventApplication) context.getApplicationContext();
        if (!BuildConfig.RELEASE) {
            return resources.getString(R.string.brevent_about_version_summary_debug,
                    BuildConfig.VERSION_NAME);
        } else if (application.mDaemonTime > 0) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            String normal = resources.getString(R.string.brevent_about_version_mode_normal);
            String root = resources.getString(R.string.brevent_about_version_mode_root);
            return resources.getString(R.string.brevent_about_version_summary,
                    BuildConfig.VERSION_NAME, getVersion(context),
                    HideApiOverride.isShell(application.mUid) ? normal :
                            (HideApiOverride.isRoot(application.mUid) ? root :
                                    resources.getString(android.R.string.unknownName)),
                    format.format(application.mDaemonTime),
                    format.format(application.mServerTime)
            );
        } else {
            return BuildConfig.VERSION_NAME;
        }
    }

    private String getVersion(Context context) {
        PackageManager packageManager = context.getPackageManager();
        String installer = packageManager.getInstallerPackageName(BuildConfig.APPLICATION_ID);
        if (((BreventSettings) context).isPlay()) {
            if (DonateActivity.PACKAGE_PLAY.equals(installer)) {
                return context.getString(R.string.brevent_about_version_play);
            } else {
                return context.getString(R.string.brevent_about_version_like_play);
            }
        }

        if (!TextUtils.isEmpty(installer)
                && packageManager.getLaunchIntentForPackage(installer) != null) {
            if ("com.meizu.mstore".equals(installer)) {
                return context.getString(R.string.brevent_about_version_meizu);
            } else if ("com.smartisanos.appstore".equals(installer)) {
                return context.getString(R.string.brevent_about_version_smartisan);
            }
        }
        return context.getString(R.string.brevent_about_version_other);
    }

}
