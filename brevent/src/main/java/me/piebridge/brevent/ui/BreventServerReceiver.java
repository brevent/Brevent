package me.piebridge.brevent.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventIntent;

/**
 * Created by thom on 2017/4/19.
 */
public class BreventServerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        Context context = LocaleUtils.updateResources(c);
        if (BreventIntent.ACTION_HOME_TID.equals(action)) {
            int homeTid = intent.getIntExtra(BreventIntent.EXTRA_HOME_TID, 0);
            if (homeTid > 0) {
                String message = context.getResources().getString(R.string.toast_home_tid, homeTid);
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        } else if (BreventIntent.ACTION_ADD_PACKAGE.equals(action)) {
            String packageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME);
            int size = intent.getIntExtra(BreventIntent.EXTRA_BREVENT_SIZE, 0);
            CharSequence label = getLabel(context, packageName);
            if (label != null && size > 1) {
                String message = context.getResources().getString(R.string.toast_add_package,
                        label, size);
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        } else if (BreventIntent.ACTION_ALIPAY.equals(action) && BuildConfig.RELEASE) {
            showAlipay(context, intent.getStringExtra(BreventIntent.EXTRA_ALIPAY_SUM));
        } else if (BreventIntent.ACTION_ALIPAY2.equals(action)) {
            Toast.makeText(context, R.string.toast_alipay2, Toast.LENGTH_LONG).show();
        }
    }

    public static void showAlipay(Context context, String sum) {
        BreventApplication application = (BreventApplication) context.getApplicationContext();
        if (application.isUnsafe()) {
            return;
        }
        double donation = application.decode(context, sum, true);
        if (DecimalUtils.isPositive(donation)) {
            PreferencesUtils.getPreferences(context)
                    .edit().putString("alipay1", sum).apply();
        }
        String format = DecimalUtils.format(donation);
        String message = context.getResources().getString(R.string.toast_alipay, format);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private CharSequence getLabel(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
        CharSequence label = null;
        if (launchIntent == null) {
            label = applicationInfo.loadLabel(packageManager);
        } else {
            ResolveInfo resolveInfo = packageManager.resolveActivity(launchIntent, 0);
            if (resolveInfo != null) {
                label = resolveInfo.activityInfo.loadLabel(packageManager);
            }
        }
        return label;
    }

}
