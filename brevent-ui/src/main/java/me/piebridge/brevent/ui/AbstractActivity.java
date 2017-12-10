package me.piebridge.brevent.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;

import java.util.Locale;

/**
 * Created by thom on 2017/9/2.
 */
public abstract class AbstractActivity extends Activity {

    private Locale locale;

    private volatile boolean stopped;

    @Override
    @CallSuper
    protected void attachBaseContext(Context base) {
        locale = LocaleUtils.getOverrideLocale(base);
        super.attachBaseContext(LocaleUtils.updateResources(base, locale));
    }

    @Override
    @CallSuper
    protected void onResume() {
        super.onResume();
        if (LocaleUtils.isChanged(locale, this)) {
            recreate();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        stopped = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        stopped = true;
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        stopped = true;
        super.onStop();
    }

    public boolean isStopped() {
        return stopped;
    }

}
