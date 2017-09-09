package me.piebridge.brevent.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;

import java.util.Locale;

/**
 * Created by thom on 2017/9/2.
 */
public class LocaleUtils {

    private static final String OVERRIDE_LANGUAGE = "override_language";

    private LocaleUtils() {

    }

    public static Context updateResources(Context context) {
        return updateResources(context, getOverrideLocale(context));
    }

    public static Locale getOverrideLocale(Context context) {
        String language = PreferencesUtils.getDevicePreferences(context)
                .getString(OVERRIDE_LANGUAGE, "");
        return getLocale(language);
    }

    private static Locale getLocale(String language) {
        if (TextUtils.isEmpty(language)) {
            return getSystemLocale();
        }
        String[] languages = language.split("_", 2);
        if (languages.length == 1) {
            return new Locale(language);
        } else {
            return new Locale(languages[0], languages[1]);
        }
    }

    @SuppressLint("ApplySharedPref")
    public static boolean setOverrideLanguage(Context context, String language) {
        try {
            return isChanged(getLocale(language), context);
        } finally {
            PreferencesUtils.getPreferences(context)
                    .edit().putString(OVERRIDE_LANGUAGE, language).apply();
        }
    }

    public static Locale getSystemLocale() {
        Configuration configuration = Resources.getSystem().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return configuration.getLocales().get(0);
        } else {
            return getLocaleDeprecated(configuration);
        }
    }

    public static Context getSystemContext(Context context) {
        return updateResources(context, getSystemLocale());
    }

    private static Locale getLocaleDeprecated(Configuration configuration) {
        return configuration.locale;
    }

    public static Context updateResources(Context context, Locale locale) {
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLayoutDirection(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }

    public static boolean isChanged(Locale locale, Context context) {
        Locale current = getOverrideLocale(context);
        String language = locale.getLanguage();
        boolean changed;
        if (!language.equals(current.getLanguage())) {
            changed = true;
        } else if ("zh".equals(language)) {
            changed = !locale.getCountry().equals(current.getCountry());
        } else {
            changed = false;
        }
        android.util.Log.d("BreventUI", "current: " + current + ", locale: " + locale
                + ", changed: " + changed);
        return changed;
    }

}