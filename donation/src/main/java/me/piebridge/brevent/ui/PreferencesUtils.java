package me.piebridge.brevent.ui;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * Created by thom on 2017/9/6.
 */
public class PreferencesUtils {

    private PreferencesUtils() {

    }

    private static SharedPreferences getPreferences(Context context, boolean migrate) {
        Context deviceContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !context.isDeviceProtectedStorage()) {
            deviceContext = context.createDeviceProtectedStorageContext();
            if (migrate) {
                deviceContext.moveSharedPreferencesFrom(context,
                        PreferenceManager.getDefaultSharedPreferencesName(context));
            }
        } else {
            deviceContext = context;
        }
        return PreferenceManager.getDefaultSharedPreferences(deviceContext);
    }

    public static SharedPreferences getDevicePreferences(Context context) {
        return getPreferences(context, false);
    }

    public static SharedPreferences getPreferences(Context context) {
        Context applicationContext;
        if (context instanceof Activity) {
            applicationContext = ((Activity) context).getApplication();
        } else if (context instanceof Service) {
            applicationContext = ((Service) context).getApplication();
        } else {
            applicationContext = context;
        }
        return getPreferences(applicationContext, true);
    }

}
