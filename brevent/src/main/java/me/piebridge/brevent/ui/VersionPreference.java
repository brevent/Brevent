package me.piebridge.brevent.ui;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import me.piebridge.brevent.BuildConfig;

/**
 * Created by thom on 2017/3/2.
 */
public class VersionPreference extends Preference {

    public VersionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        return BuildConfig.VERSION_NAME;
    }

}
