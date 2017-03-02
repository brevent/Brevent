package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

import me.piebridge.brevent.BuildConfig;

/**
 * Created by thom on 2017/3/2.
 */
public class ProjectPreference extends Preference {

    public ProjectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        return BuildConfig.PROJECT;
    }

    @Override
    protected void onClick() {
        if (!TextUtils.isEmpty(BuildConfig.PROJECT)) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PROJECT));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            getContext().startActivity(intent);
        }
    }

}
