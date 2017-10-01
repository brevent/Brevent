package me.piebridge.brevent.ui;

import android.app.Activity;
import android.app.Application;
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

    public static SharedPreferences getDevicePreferences(Context context) {
        Context deviceContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !context.isDeviceProtectedStorage()) {
            deviceContext = context.createDeviceProtectedStorageContext();
        } else {
            deviceContext = context;
        }
        return PreferenceManager.getDefaultSharedPreferences(deviceContext);
    }

    public static SharedPreferences getPreferences(Application application) {
        return getDevicePreferences(application);
    }

    public static SharedPreferences getPreferences(Activity activity) {
        return getDevicePreferences(activity.getApplication());
    }

    public static SharedPreferences getPreferences(Service service) {
        return getDevicePreferences(service.getApplication());
    }

}
