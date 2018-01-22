package me.piebridge.brevent.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventPackages;
import me.piebridge.brevent.protocol.BreventProtocol;
import me.piebridge.brevent.protocol.BreventRequest;

/**
 * Created by thom on 2017/4/19.
 */
public class BreventServerReceiver extends BroadcastReceiver {

    private ExecutorService executor = new ScheduledThreadPoolExecutor(0x1);

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        UILog.i("received: " + action);
        Context context = LocaleUtils.updateResources(c);
        Resources resources = context.getResources();
        if (BreventIntent.ACTION_HOME_TID.equals(action)) {
            int homeTid = intent.getIntExtra(BreventIntent.EXTRA_HOME_TID, 0);
            if (homeTid > 0) {
                String message = resources.getString(R.string.toast_home_tid, homeTid);
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        } else if (BreventIntent.ACTION_ADD_PACKAGE.equals(action)) {
            PackageInfo packageInfo = intent.getParcelableExtra(BreventIntent.EXTRA_PACKAGE_INFO);
            String token = intent.getStringExtra(Intent.EXTRA_REMOTE_INTENT_TOKEN);
            showBrevented(context, packageInfo, token);
        } else if (BreventIntent.ACTION_ALIPAY.equals(action) && BuildConfig.RELEASE) {
            Context applicationContext = context.getApplicationContext();
            if (applicationContext instanceof BreventApplication) {
                BreventApplication application = (BreventApplication) applicationContext;
                showAlipay(application, intent.getStringExtra(BreventIntent.EXTRA_ALIPAY_SUM),
                        intent.getBooleanExtra(BreventIntent.EXTRA_ALIPAY_SIN, false));
            }
        } else if (BreventIntent.ACTION_ALIPAY3.equals(action)) {
            showAlipay2(context, intent.getBooleanExtra(BreventIntent.EXTRA_ALIPAY, false));
        } else if (BreventIntent.ACTION_BREVENT.equals(action)) {
            Context applicationContext = context.getApplicationContext();
            if (applicationContext instanceof BreventApplication) {
                ((BreventApplication) applicationContext).onStarted();
                checkPort(intent.getStringExtra(Intent.EXTRA_REMOTE_INTENT_TOKEN));
            }
        } else if (BreventIntent.ACTION_RESTORE.equals(action)) {
            String packageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME);
            String label = intent.getStringExtra(Intent.EXTRA_REFERRER_NAME);
            String token = intent.getStringExtra(Intent.EXTRA_REMOTE_INTENT_TOKEN);
            restore(context, packageName, label, token);
        }
    }

    private void restore(Context context, String packageName, String label, String token) {
        Future<Boolean> future = executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return doRestore(packageName, token);
            }
        });

        Boolean success = null;
        try {
            success = future.get(0x5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            UILog.w(BreventApplication.formatBreventException(e));
        } catch (InterruptedException | ExecutionException e) {
            UILog.w("future exception", e);
        }

        int resId;
        if (Objects.equals(Boolean.TRUE, success)) {
            resId = R.string.unbrevented_app;
        } else {
            resId = R.string.unbrevented_app_fail;
        }
        String message = context.getResources().getString(resId, label);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    boolean doRestore(String packageName, String token) {
        BreventPackages request = new BreventPackages(false, Collections.singleton(packageName));
        request.undoable = false;
        request.token = token;
        BreventProtocol response = null;
        try (
                Socket socket = new Socket(InetAddress.getLoopbackAddress(), BreventProtocol.PORT);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                DataInputStream is = new DataInputStream(socket.getInputStream())
        ) {
            BreventProtocol.writeTo(request, os);
            os.flush();
            response = BreventProtocol.readFrom(is);
        } catch (IOException e) {
            UILog.w(BreventApplication.formatBreventException(e), e);
        }
        return (response != null && response instanceof BreventPackages);
    }

    static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void showBrevented(Context context, PackageInfo packageInfo, String token) {
        String label = AppsLabelLoader.loadLabel(context.getPackageManager(), packageInfo);
        NotificationManager nm = BreventIntentService.getNotificationManager(context);
        Notification.Builder builder = BreventIntentService.buildNotification(context, "brevent",
                NotificationManager.IMPORTANCE_HIGH, Notification.PRIORITY_HIGH);
        builder.setAutoCancel(true);
        builder.setGroup("brevented");
        builder.setGroupSummary(true);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setSmallIcon(BuildConfig.IC_STAT);
        builder.setLargeIcon(drawableToBitmap(AppsIconTask.loadIcon(context, packageInfo)));
        builder.setContentTitle(context.getString(R.string.brevented_app, label));
        builder.setContentText(context.getString(R.string.unbrevent_app));

        Intent intent = new Intent(context, BreventServerReceiver.class);
        intent.setAction(BreventIntent.ACTION_RESTORE);
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageInfo.packageName);
        intent.putExtra(Intent.EXTRA_REMOTE_INTENT_TOKEN, token);
        intent.putExtra(Intent.EXTRA_REFERRER_NAME, label);
        PendingIntent restore = PendingIntent.getBroadcast(context,
                packageInfo.packageName.hashCode(), intent, PendingIntent.FLAG_ONE_SHOT);
        builder.setContentIntent(restore);
        nm.notify(packageInfo.packageName, packageInfo.packageName.hashCode(), builder.build());
    }

    private void showAlipay2(Context context, boolean ok) {
        String message = context.getString(ok ? R.string.toast_alipay_ok : R.string.toast_alipay_ko);
        if (!TextUtils.isEmpty(message)) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    public static void showAlipay(BreventApplication application, String sum, boolean sin) {
        if (application.isUnsafe()) {
            return;
        }
        double donation = BreventApplication.decode(application, sum, true, true);
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

    void checkPort(final String token) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                checkToken(token);
            }
        });
    }

    void checkToken(String token) {
        try (
                Socket socket = new Socket(InetAddress.getLoopbackAddress(), BreventProtocol.PORT);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                DataInputStream is = new DataInputStream(socket.getInputStream())
        ) {
            BreventRequest breventRequest = new BreventRequest();
            breventRequest.token = token;
            BreventProtocol.writeTo(breventRequest, os);
            os.flush();
            BreventProtocol.readFrom(is);
        } catch (IOException e) {
            UILog.w(BreventApplication.formatBreventException(e), e);
        }
    }

}
