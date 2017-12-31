package me.piebridge.brevent.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventProtocol;
import me.piebridge.brevent.protocol.BreventRequest;

/**
 * Created by thom on 2017/4/19.
 */
public class BreventServerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        UILog.d("received: " + action);
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
            Context applicationContext = context.getApplicationContext();
            if (applicationContext instanceof BreventApplication) {
                BreventApplication application = (BreventApplication) applicationContext;
                showAlipay(application, intent.getStringExtra(BreventIntent.EXTRA_ALIPAY_SUM),
                        intent.getBooleanExtra(BreventIntent.EXTRA_ALIPAY_SIN, false));
            }
        } else if (BreventIntent.ACTION_ALIPAY2.equals(action)) {
            Toast.makeText(context, R.string.toast_alipay2, Toast.LENGTH_LONG).show();
        } else if (BreventIntent.ACTION_BREVENT.equals(action)) {
            Context applicationContext = context.getApplicationContext();
            if (applicationContext instanceof BreventApplication) {
                ((BreventApplication) applicationContext).onStarted();
                checkPort(intent.getStringExtra(Intent.EXTRA_REMOTE_INTENT_TOKEN));
            }
        }
    }

    public static void showAlipay(BreventApplication application, String sum, boolean sin) {
        if (application.isUnsafe()) {
            return;
        }
        double donation = BreventApplication.decode(application, sum, true);
        if (DecimalUtils.isPositive(donation)) {
            PreferencesUtils.getPreferences(application)
                    .edit().putString("alipay1", sum).apply();
        } else {
            PreferencesUtils.getPreferences(application)
                    .edit().remove("alipay1").apply();
        }
        String format = DecimalUtils.format(donation);
        if (!"0".equals(format)) {
            int resId = sin ? R.string.toast_alipay_single : R.string.toast_alipay;
            String message = application.getResources().getString(resId, format);
            Toast.makeText(application, message, Toast.LENGTH_LONG).show();
        }
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

    void checkPort(final String token) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                checkToken(token);
            }
        }).start();
    }

    void checkToken(String token) {
        try (
                Socket socket = new Socket(InetAddress.getLoopbackAddress(), BreventProtocol.PORT);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                DataInputStream is = new DataInputStream(socket.getInputStream())
        ) {
            BreventProtocol.writeTo(new BreventRequest(false, token), os);
            os.flush();
            BreventProtocol.readFrom(is);
        } catch (IOException ignore) {
            // do nothing
        }
    }

}
