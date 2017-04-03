package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.res.Resources;
import android.preference.Preference;
import android.util.AttributeSet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.override.HideApiOverride;

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
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String normal = resources.getString(R.string.brevent_about_version_mode_normal);
        return resources.getString(R.string.brevent_about_version_summary,
                BuildConfig.VERSION_NAME,
                HideApiOverride.isShell(application.mUid) ?  normal:
                        (HideApiOverride.isRoot(application.mUid) ? "ROOT" :
                                resources.getString(android.R.string.unknownName)),
                format.format(application.mDaemonTime)
        );
    }

}
