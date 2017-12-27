package me.piebridge.brevent.ui;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accounts.NetworkErrorException;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.util.ArraySet;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.telecom.TelecomManager;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.SearchView;
import android.widget.Toolbar;

import com.android.internal.statusbar.IStatusBarService;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import dalvik.system.PathClassLoader;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.override.HideApiOverride;
import me.piebridge.brevent.override.HideApiOverrideM;
import me.piebridge.brevent.override.HideApiOverrideN;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventNoEvent;
import me.piebridge.brevent.protocol.BreventOK;
import me.piebridge.brevent.protocol.BreventPackages;
import me.piebridge.brevent.protocol.BreventPriority;
import me.piebridge.brevent.protocol.BreventProtocol;
import me.piebridge.brevent.protocol.BreventResponse;
import me.piebridge.stats.StatsUtils;

public class BreventActivity extends AbstractActivity
        implements ViewPager.OnPageChangeListener, SwipeRefreshLayout.OnRefreshListener,
        View.OnClickListener, SearchView.OnCloseListener, SearchView.OnQueryTextListener {

    private static final int DELAY = 1000;

    private static final int DELAY5 = 5000;

    private static final int DELAY15 = 15000;

    public static final long BEGIN = 1415750400_000L;

    private static final String GMS = "com.google.android.gms";

    private static final String GMS_VALID = "gms-valid";

    private static final String GMS_LAST_UPDATE = "gms-last-update";

    private static final byte[][] GMS_SIGNATURES = {
            {56, -111, -118, 69, 61, 7, 25, -109, 84, -8,
                    -79, -102, -16, 94, -58, 86, 44, -19, 87, -120},
            {88, -31, -60, 19, 63, 116, 65, -20, 61, 44,
                    39, 2, 112, -95, 72, 2, -38, 71, -70, 14}
    };

    public static final int MESSAGE_RETRIEVE = 0;
    public static final int MESSAGE_RETRIEVE2 = 1;
    public static final int MESSAGE_BREVENT_RESPONSE = 2;
    public static final int MESSAGE_BREVENT_NO_RESPONSE = 3;
    public static final int MESSAGE_BREVENT_REQUEST = 4;
    public static final int MESSAGE_ROOT_COMPLETED = 5;
    public static final int MESSAGE_LOGS = 6;
    public static final int MESSAGE_REMOVE_ADB = 7;

    public static final int UI_MESSAGE_SHOW_PROGRESS = 0;
    public static final int UI_MESSAGE_HIDE_PROGRESS = 1;
    public static final int UI_MESSAGE_SHOW_PAGER = 2;
    public static final int UI_MESSAGE_SHOW_FRAGMENT = 3;
    public static final int UI_MESSAGE_NO_BREVENT = 4;
    public static final int UI_MESSAGE_IO_BREVENT = 5;
    public static final int UI_MESSAGE_NO_BREVENT_DATA = 6;
    public static final int UI_MESSAGE_UPDATE_BREVENT = 7;
    public static final int UI_MESSAGE_HIDE_DISABLED = 8;
    public static final int UI_MESSAGE_UPDATE_PRIORITY = 9;
    public static final int UI_MESSAGE_SHOW_SUCCESS = 10;
    public static final int UI_MESSAGE_NO_EVENT = 11;
    public static final int UI_MESSAGE_NO_PERMISSION = 12;
    public static final int UI_MESSAGE_MAKE_EVENT = 13;
    public static final int UI_MESSAGE_LOGS = 14;
    public static final int UI_MESSAGE_ROOT_COMPLETED = 15;
    public static final int UI_MESSAGE_SHELL_COMPLETED = 16;
    public static final int UI_MESSAGE_SHOW_PROGRESS_ADB = 17;
    public static final int UI_MESSAGE_CHECKING_BREVENT = 18;
    public static final int UI_MESSAGE_NO_LOCAL_NETWORK = 19;
    public static final int UI_MESSAGE_CHECKED_BREVENT = 20;

    public static final int IMPORTANT_INPUT = 0;
    public static final int IMPORTANT_ALARM = 1;
    public static final int IMPORTANT_SMS = 2;
    public static final int IMPORTANT_HOME = 3;
    public static final int IMPORTANT_PERSISTENT = 4;
    public static final int IMPORTANT_ANDROID = 5;
    public static final int IMPORTANT_DIALER = 6;
    public static final int IMPORTANT_ASSISTANT = 7;
    public static final int IMPORTANT_WEBVIEW = 8;
    public static final int IMPORTANT_ACCESSIBILITY = 9;
    public static final int IMPORTANT_DEVICE_ADMIN = 10;
    public static final int IMPORTANT_BATTERY = 11;
    public static final int IMPORTANT_TRUST_AGENT = 12;
    public static final int IMPORTANT_GMS = 13;
    public static final int IMPORTANT_WALLPAPER = 14;

    private static final String FRAGMENT_DISABLED = "disabled";
    private static final String FRAGMENT_PROGRESS = "progress";
    private static final String FRAGMENT_PROGRESS_APPS = "progress_apps";
    private static final String FRAGMENT_FEEDBACK = "feedback";
    private static final String FRAGMENT_UNSUPPORTED = "unsupported";
    private static final String FRAGMENT_REPORT = "report";
    private static final String FRAGMENT_PROGRESS2 = "progress2";
    private static final String FRAGMENT_SORT = "sort";
    private static final String FRAGMENT_PAYMENT = "payment";

    private static final String PACKAGE_FRAMEWORK = "android";
    private Signature[] frameworkSignatures;
    private boolean fakeFramework;

    static final int REQUEST_CODE_SETTINGS = 1;

    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";

    private static final int STATUS_BREVENT = 0x1;
    private static final int STATUS_UNBREVENT = 0x2;
    private static final int STATUS_IMPORTANT = 0x4;
    private static final int STATUS_MASK = 0x1 | 0x2;

    private ViewPager mPager;

    private AppsPagerAdapter mAdapter;

    private String[] mTitles;

    private SimpleArrayMap<String, SparseIntArray> mProcesses = new SimpleArrayMap<>();
    private Set<String> mBrevent = new ArraySet<>();
    private Set<String> mPriority = new ArraySet<>();
    private SimpleArrayMap<String, Integer> mImportant = new SimpleArrayMap<>();
    private SimpleArrayMap<String, Integer> mFavorite = new SimpleArrayMap<>();
    private volatile SimpleArrayMap<String, UsageStats> mStats = null;
    private Set<String> mGcm = new ArraySet<>();
    Set<String> mPackages = new ArraySet<>();
    private String mVpn;

    private int mSelectStatus;

    private CoordinatorLayout mCoordinator;
    private Toolbar mToolbar;
    private Snackbar mSnackBar;

    private Handler mHandler;
    private Handler uiHandler;

    @ColorInt
    int mColorControlNormal;
    @ColorInt
    int mTextColorPrimary;
    @ColorInt
    int mColorControlHighlight;

    private String mLauncher;

    private String mSms;

    private String mDialer;

    private volatile boolean hasResponse;

    private UsbConnectedReceiver mConnectedReceiver;

    private final Object updateLock = new Object();

    private boolean notificationEventMade;

    private SearchView mSearchView;
    private String mQuery;

    private Boolean check;
    private BreventConfiguration mConfiguration;
    private boolean shouldUpdateConfiguration;
    private boolean shouldOpenSettings;
    private boolean force;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.RELEASE) {
            StatsUtils.init(getApplication());
        }
        boolean disabledXposed = !BuildConfig.RELEASE;
        if (BuildConfig.SERVER != null) {
            String clazzServer = String.valueOf(BuildConfig.SERVER);
            try {
                ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                String sourceDir = getPackageManager()
                        .getApplicationInfo(BuildConfig.APPLICATION_ID, 0).sourceDir;
                PathClassLoader classLoader = new PathClassLoader(sourceDir, systemClassLoader);
                classLoader.loadClass(clazzServer).getMethod(String.valueOf('b')).invoke(null);
                disabledXposed = true;
            } catch (Exception t) {
                if (t instanceof InvocationTargetException) {
                    Throwable throwable = ((InvocationTargetException) t).getTargetException();
                    if (throwable instanceof NoClassDefFoundError) {
                        StackTraceElement[] elements = throwable.getStackTrace();
                        int l = elements.length - 1;
                        if (l > 0x7 && elements[0].getClassName().equals(clazzServer) &&
                                elements[l].getClassName().startsWith("com.android.internal.os")) {
                            disabledXposed = true;
                        }
                    }
                }
                if (!disabledXposed) {
                    UILog.d("Can't disable Xposed", t);
                }
            }
        }
        if (!disabledXposed && !BuildConfig.DEBUG) {
            showUnsupported(R.string.unsupported_xposed);
        } else if (!BreventApplication.IS_OWNER) {
            showUnsupported(R.string.unsupported_owner);
        } else if (!verifySignature()) {
            showUnsupported(R.string.unsupported_signature);
        } else if (isFlymeClone()) {
            showUnsupported(R.string.unsupported_clone);
        } else if (shouldShowGuide()) {
            openGuide("first");
            super.finish();
        } else {
            setContentView(R.layout.activity_brevent);

            mCoordinator = findViewById(R.id.coordinator);
            mToolbar = findViewById(R.id.toolbar);
            setActionBar(mToolbar);

            mPager = findViewById(R.id.pager);
            mPager.addOnPageChangeListener(this);
            mPager.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    setRefreshEnabled(false);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            setRefreshEnabled(true);
                            break;
                    }
                    return false;
                }
            });
            mPager.setVisibility(View.INVISIBLE);

            uiHandler = new AppsActivityUIHandler(this);
            mHandler = new AppsActivityHandler(this, uiHandler);

            mTitles = getResources().getStringArray(R.array.fragment_apps);

            fakeFramework = isBreventFramework();

            mColorControlNormal = ColorUtils.resolveColor(this, android.R.attr.colorControlNormal);
            mTextColorPrimary = ColorUtils.resolveColor(this, android.R.attr.textColorPrimary);
            mColorControlHighlight = ColorUtils.resolveColor(this,
                    android.R.attr.colorControlHighlight);

            uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_SHOW_PROGRESS);

            updateCheck();
        }
    }

    private boolean shouldShowGuide() {
        Intent intent = getIntent();
        return intent != null && Intent.ACTION_MAIN.equals(intent.getAction())
                && PreferencesUtils.getPreferences(this).getBoolean(BreventGuide.GUIDE, true);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (uiHandler != null && ((BreventApplication) getApplication()).isRunningAsRoot()) {
            uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_SHOW_PROGRESS);
        }
    }

    private boolean isFlymeClone() {
        return BuildConfig.RELEASE &&
                getIntent().getIntExtra(String.valueOf(BuildConfig.FLYME_CLONE), 0) != 0;
    }

    public void showUnsupported(int resId) {
        showUnsupported(resId, false);
    }

    public void showUnsupported(int resId, boolean exit) {
        UnsupportedFragment fragment = new UnsupportedFragment();
        fragment.setMessage(resId);
        fragment.show(getFragmentManager(), FRAGMENT_UNSUPPORTED);
        if (exit) {
            mHandler.removeCallbacksAndMessages(null);
            uiHandler.removeCallbacksAndMessages(null);
        }
    }

    public void showPayment(int days, int size, int required) {
        hideDisabled();
        hideProgress();
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("show " + FRAGMENT_PAYMENT + ", stopped: " + isStopped());
        }
        if (isStopped()) {
            return;
        }
        AppsPaymentFragment fragment = (AppsPaymentFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_PAYMENT);
        if (fragment != null) {
            fragment.dismiss();
        }
        fragment = new AppsPaymentFragment();
        fragment.setMessage(days, size, required);
        fragment.show(getFragmentManager(), FRAGMENT_PAYMENT);
    }

    public void showSort() {
        if (isStopped()) {
            return;
        }
        SortFragment fragment = (SortFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_SORT);
        if (fragment != null) {
            fragment.dismiss();
        }
        fragment = new SortFragment();
        fragment.show(getFragmentManager(), FRAGMENT_SORT);
    }

    private boolean verifySignature() {
        if (!BuildConfig.RELEASE) {
            return true;
        }
        try {
            String sourceDir = getPackageManager()
                    .getApplicationInfo(BuildConfig.APPLICATION_ID, 0).sourceDir;
            return Arrays.equals(BuildConfig.SIGNATURE, BreventProtocol.getFingerprint(sourceDir));
        } catch (PackageManager.NameNotFoundException e) { // NOSONAR
            UILog.d("Can't find " + BuildConfig.APPLICATION_ID);
            return false;
        }
    }

    public boolean hasGms() {
        PackageManager packageManager = getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(GMS, 0);
            if (!packageInfo.applicationInfo.enabled) {
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) { // NOSONAR
            // do nothing
            return false;
        }

        SharedPreferences preferences = getSharedPreferences("signature", Context.MODE_PRIVATE);
        long lastUpdate = preferences.getLong(GMS_LAST_UPDATE, 0);
        if (preferences.contains(GMS_VALID) && packageInfo.lastUpdateTime == lastUpdate) {
            return preferences.getBoolean(GMS_VALID, false);
        }

        boolean valid = checkGms(packageInfo.applicationInfo.sourceDir);
        preferences.edit()
                .putBoolean(GMS_VALID, valid)
                .putLong(GMS_LAST_UPDATE, packageInfo.lastUpdateTime)
                .apply();
        return valid;
    }

    private boolean checkGms(String sourceDir) {
        byte[] sha1 = BreventProtocol.getFingerprint(sourceDir);
        for (byte[] bytes : GMS_SIGNATURES) {
            if (Arrays.equals(sha1, bytes)) {
                return true;
            }
        }
        return false;
    }

    public void showDisabled() {
        hideProgress();
        showDisabled(R.string.brevent_service_start);
    }

    public void showDisabled(int title) {
        showDisabled(title, force);
        if (force) {
            force = false;
        }
    }

    public void showDisabled(int title, boolean force) {
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("show " + FRAGMENT_DISABLED + ", " + title + ": " + title + ", force: " + force
                    + ", stopped: " + isStopped());
        }
        if (isStopped()) {
            return;
        }
        if (mConnectedReceiver == null) {
            mConnectedReceiver = new UsbConnectedReceiver(this);
            IntentFilter filter = new IntentFilter(HideApiOverride.ACTION_USB_STATE);
            registerReceiver(mConnectedReceiver, filter);
        }
        AppsDisabledFragment fragment = (AppsDisabledFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_DISABLED);
        if (force || fragment == null || fragment.getTitle() != title) {
            if (fragment != null) {
                fragment.dismiss();
            }
            fragment = new AppsDisabledFragment();
            fragment.setTitle(title);
            fragment.show(getFragmentManager(), FRAGMENT_DISABLED);
        }
        if (hasResponse) {
            mHandler.sendEmptyMessage(MESSAGE_RETRIEVE);
        }
    }

    public void hideDisabled() {
        dismissDialog(FRAGMENT_DISABLED, false);
    }

    public void hideReport() {
        dismissDialog(FRAGMENT_REPORT, false);
    }

    public void showProgress(int message) {
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("show " + FRAGMENT_PROGRESS + ", " + message + ": " + message
                    + ", stopped: " + isStopped());
        }
        if (isStopped()) {
            return;
        }
        hideDisabled();
        ProgressFragment fragment = (ProgressFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_PROGRESS);
        if (fragment == null || fragment.getMessage() != message) {
            if (fragment != null) {
                fragment.dismiss();
            }
            fragment = new ProgressFragment();
            fragment.setMessage(message);
            fragment.show(getFragmentManager(), FRAGMENT_PROGRESS);
        }
    }

    public void showProcessChecking() {
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("show " + FRAGMENT_PROGRESS2 + ", stopped: " + isStopped());
        }
        if (isStopped()) {
            return;
        }
        hideDisabled();
        ProgressFragment fragment = (ProgressFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_PROGRESS2);
        if (fragment != null) {
            fragment.dismiss();
        }
        fragment = new ProgressFragment();
        fragment.setMessage(R.string.process_checking);
        fragment.show(getFragmentManager(), FRAGMENT_PROGRESS2);
    }

    public void hideProcessChecking() {
        dismissDialog(FRAGMENT_PROGRESS2, false);
    }

    public AppsProgressFragment showAppProgress() {
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("show " + FRAGMENT_PROGRESS_APPS + ", stopped: " + isStopped());
        }
        if (isStopped()) {
            return null;
        }
        AppsProgressFragment fragment = (AppsProgressFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_PROGRESS_APPS);
        if (fragment != null) {
            fragment.dismiss();
        }
        fragment = new AppsProgressFragment();
        fragment.show(getFragmentManager(), FRAGMENT_PROGRESS_APPS);
        return fragment;
    }

    public void updateAppProgress(int progress, int max, int size) {
        if (isStopped()) {
            return;
        }
        AppsProgressFragment fragment = (AppsProgressFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_PROGRESS_APPS);
        if (fragment == null) {
            fragment = new AppsProgressFragment();
            fragment.show(getFragmentManager(), FRAGMENT_PROGRESS_APPS);
        }
        fragment.update(progress, max, size);
    }

    public void hideProgress() {
        dismissDialog(FRAGMENT_PROGRESS, false);
    }

    public void hideAppProgress() {
        dismissDialog(FRAGMENT_PROGRESS, false);
        dismissDialog(FRAGMENT_PROGRESS_APPS, false);
    }

    @Override
    protected void onStop() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            uiHandler.removeCallbacksAndMessages(null);
        }
        unregisterReceiver();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(R.string.brevent);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        force = true;
        if (mHandler != null) {
            if (((BreventApplication) getApplication()).isRunningAsRoot()) {
                mHandler.sendEmptyMessage(MESSAGE_RETRIEVE2);
            } else {
                mHandler.sendEmptyMessage(MESSAGE_RETRIEVE);
            }
        }
        if (BuildConfig.RELEASE) {
            ((BreventApplication) getApplication()).decodeFromClipboard();
        }
        if (BreventIntent.ACTION_FEEDBACK.equals(getIntent().getAction())) {
            String path = getIntent().getStringExtra(BreventIntent.EXTRA_PATH);
            UILog.d("path: " + path);
            if (hasEmailClient(this)) {
                String content = Build.FINGERPRINT + "\n" +
                        getString(R.string.brevent_status_stopped);
                sendEmail(this, new File(path), content);
            }
        }
        if (shouldUpdateConfiguration) {
            updateConfiguration(true);
        }
    }

    public static PendingIntent getAlarmPendingIntent(Context context) {
        Intent intent = new Intent(context, BreventReceiver.class);
        intent.setAction(BreventIntent.ACTION_ALARM);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static void setAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, getAlarmPendingIntent(context));
        UILog.d("setAlarm");
    }

    public static void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getAlarmPendingIntent(context));
        UILog.d("cancelAlarm");
    }

    private void dismissDialog(String tag, boolean allowStateLoss) {
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
                UILog.d("dismiss " + tag + ", " + allowStateLoss + ": " + allowStateLoss
                        + ", stopped: " + isStopped());
            }
            if (allowStateLoss) {
                fragment.dismissAllowingStateLoss();
            } else if (!isStopped()) {
                fragment.dismiss();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            Looper looper = mHandler.getLooper();
            if (looper != null) {
                looper.quit();
            }
            mHandler = null;
            uiHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // do nothing
    }

    @Override
    public void onPageSelected(int position) {
        AppsFragment fragment = mAdapter.getFragment(position);
        if (fragment != null) {
            setSelectCount(fragment.getSelectedSize());
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // do nothing
    }

    public int getStatus(String packageName) {
        SparseIntArray status;
        synchronized (updateLock) {
            status = mProcesses.get(packageName);
        }
        if (status == null) {
            return AppsInfo.STATUS_STOPPED;
        } else if (BreventResponse.isStandby(status)) {
            return AppsInfo.STATUS_STANDBY;
        } else {
            return AppsInfo.STATUS_RUNNING;
        }
    }

    public String getDescription(String packageName) {
        SparseIntArray status;
        synchronized (updateLock) {
            status = mProcesses.get(packageName);
        }
        if (status == null) {
            return null;
        }
        int cached = 0;
        int service = 0;
        int top = 0;
        int total = 0;

        int size = status.size();
        for (int i = 0; i < size; ++i) {
            int processState = status.keyAt(i);
            if (BreventResponse.isProcess(processState)) {
                total++;
                if (BreventResponse.isTop(processState)) {
                    top++;
                } else if (BreventResponse.isService(processState)) {
                    service++;
                } else if (BreventResponse.isCached(processState)) {
                    cached++;
                }
            }
        }

        if (top == total) {
            return getString(R.string.process_all_top, top)
                    + getResources().getQuantityString(R.plurals.process_process, top);
        } else if (service == total) {
            return getString(R.string.process_all_service, service)
                    + getResources().getQuantityString(R.plurals.process_process, service);
        } else if (cached == total) {
            return getString(R.string.process_all_cached, cached)
                    + getResources().getQuantityString(R.plurals.process_process, cached);
        } else if (top == 0 && service == 0 && cached == 0) {
            return getString(R.string.process_all_total, total)
                    + getResources().getQuantityString(R.plurals.process_process, total);
        } else {
            StringBuilder sb = new StringBuilder();
            if (service > 0) {
                sb.append(getString(R.string.process_service, service));
                sb.append(", ");
            }
            if (total > 0) {
                sb.append(getString(R.string.process_total, total));
            }
            sb.append(getResources().getQuantityString(R.plurals.process_process, total));
            return sb.toString();
        }
    }

    @DrawableRes
    public int getStatusIcon(String packageName) {
        if (!mBrevent.contains(packageName)) {
            return 0;
        }
        if (Objects.equals(packageName, mVpn)) {
            return R.drawable.ic_vpn_key_black_24dp;
        }
        SparseIntArray status;
        synchronized (updateLock) {
            status = mProcesses.get(packageName);
        }
        if (BreventResponse.isAudio(status)) {
            return R.drawable.ic_play_circle_outline_black_24dp;
        } else if (BreventResponse.isAudioPaused(status)) {
            return R.drawable.ic_pause_circle_outline_black_24dp;
        } else if (isFavorite(packageName)) {
            return R.drawable.ic_favorite_border_black_24dp;
        } else if (status != null && BreventResponse.isSafe(status)) {
            return R.drawable.ic_panorama_fish_eye_black_24dp;
        } else if (BreventResponse.isStandby(status)) {
            return R.drawable.ic_snooze_black_24dp;
        } else if (BreventResponse.isRunning(status)) {
            return R.drawable.ic_alarm_black_24dp;
        } else {
            return R.drawable.ic_block_black_24dp;
        }
    }

    public boolean isPriority(String packageName) {
        return mPriority.contains(packageName);
    }

    public int getInactive(String packageName) {
        synchronized (updateLock) {
            return BreventResponse.getInactive(mProcesses.get(packageName));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (mSelectStatus != 0) {
            inflater.inflate(R.menu.menu_brevent, menu);
            if ((mSelectStatus & STATUS_BREVENT) != 0) {
                menu.findItem(R.id.action_restore).getIcon().setTint(mColorControlNormal);
            } else {
                menu.removeItem(R.id.action_restore);
            }
            if ((mSelectStatus & STATUS_UNBREVENT) != 0) {
                menu.findItem(R.id.action_brevent).getIcon().setTint(mColorControlNormal);
            } else {
                menu.removeItem(R.id.action_brevent);
            }
            if (mSearchView != null) {
                mSearchView.clearFocus();
                mSearchView = null;
            }
        } else {
            inflater.inflate(R.menu.menu_default, menu);
            if (!BuildConfig.RELEASE) {
                menu.removeItem(R.id.action_feedback);
                menu.removeItem(R.id.action_logs);
            } else if (!canFetchLogs()) {
                menu.removeItem(R.id.action_logs);
            }
            MenuItem searchItem = menu.findItem(R.id.action_search);
            searchItem.getIcon().setTint(mColorControlNormal);
            mSearchView = (SearchView) searchItem.getActionView();
            mSearchView.setOnSearchClickListener(this);
            mSearchView.setOnCloseListener(this);
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setMaxWidth(Integer.MAX_VALUE);
            if (mQuery != null) {
                mSearchView.setQuery(mQuery, false);
                mSearchView.setIconified(false);
                mSearchView.clearFocus();
            }
            MenuItem sortItem = menu.findItem(R.id.action_sort);
            sortItem.getIcon().setTint(mColorControlNormal);
        }
        return true;
    }

    public boolean canFetchLogs() {
        return BuildConfig.RELEASE && hasEmailClient(this) &&
                getPackageManager().checkPermission(Manifest.permission.READ_LOGS,
                        BuildConfig.APPLICATION_ID) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onClickHome();
                break;
            case R.id.action_restore:
                updateBrevent(false);
                break;
            case R.id.action_brevent:
                updateBrevent(true);
                break;
            case R.id.action_select_important:
                selectImportant();
                break;
            case R.id.action_select_inverse:
                selectInverse();
                break;
            case R.id.action_feedback:
                if (BuildConfig.RELEASE) {
                    openFeedback();
                }
                break;
            case R.id.action_guide:
                openGuide("menu");
                break;
            case R.id.action_logs:
                fetchLogs();
                break;
            case R.id.action_settings:
                openSettings();
                break;
            case R.id.action_sort:
                showSort();
                break;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    public void fetchLogs() {
        showProgress(R.string.process_retrieving_logs);
        mHandler.sendEmptyMessage(MESSAGE_LOGS);
    }

    public void openGuide(String type) {
        startActivity(new Intent(this, BreventGuide.class));
        if (BuildConfig.RELEASE) {
            String installer = ((BreventApplication) getApplication()).getInstaller();
            StatsUtils.logGuide(type, installer);
        }
    }

    private void openFeedback() {
        new AppsFeedbackFragment().show(getFragmentManager(), FRAGMENT_FEEDBACK);
    }

    @Override
    @CallSuper
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS) {
            UILog.d("onActivityResult");
            if (isStopped()) {
                shouldUpdateConfiguration = true;
            } else {
                updateConfiguration(false);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateConfiguration(boolean inResume) {
        shouldUpdateConfiguration = false;
        SharedPreferences preferences = PreferencesUtils.getPreferences(this);
        if (mConfiguration == null || mConfiguration.update(new BreventConfiguration(preferences))) {
            doUpdateConfiguration();
        }
        if (updateCheck() && !inResume) {
            mHandler.sendEmptyMessage(MESSAGE_RETRIEVE2);
        }
        if (mAdapter != null && updateAdapter(mAdapter)) {
            mAdapter.refreshFragment();
        }
    }

    @Override
    public void onBackPressed() {
        if (resetSearchView()) {
            return;
        }
        if (mAdapter != null) {
            AppsFragment fragment = getFragment();
            if (fragment != null && fragment.getSelectedSize() > 0) {
                fragment.clearSelected();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finishAndRemoveTask();
    }

    private void doUpdateConfiguration() {
        SharedPreferences preferences = PreferencesUtils.getPreferences(this);
        BreventApplication application = (BreventApplication) getApplication();
        if (!"forcestop_only".equals(preferences.getString(BreventConfiguration.BREVENT_METHOD, ""))
                && !application.supportStandby()) {
            preferences.edit()
                    .putString(BreventConfiguration.BREVENT_METHOD, "forcestop_only").apply();
        }
        BreventConfiguration configuration = new BreventConfiguration(preferences);
        if (BuildConfig.RELEASE) {
            configuration.androidId = BreventApplication.getId(application);
        }
        mHandler.obtainMessage(MESSAGE_BREVENT_REQUEST, configuration).sendToTarget();
    }

    public void openSettings() {
        Intent intent = new Intent(this, BreventSettings.class);
        intent.putExtra(BreventIntent.EXTRA_BREVENT_SIZE, mBrevent.size());
        startActivityForResult(intent, REQUEST_CODE_SETTINGS);
    }

    private void selectInverse() {
        getFragment().selectInverse();
    }

    private void selectImportant() {
        AppsFragment fragment = getFragment();
        fragment.select(getImportant());
    }

    private Collection<String> getImportant() {
        Set<String> important = new ArraySet<>();
        int size = mImportant.size();
        for (int i = 0; i < size; ++i) {
            important.add(mImportant.keyAt(i));
        }
        size = mFavorite.size();
        for (int i = 0; i < size; ++i) {
            important.add(mFavorite.keyAt(i));
        }
        return important;
    }

    public boolean isImportant(String packageName) {
        return mImportant.containsKey(packageName);
    }

    public boolean isGcm(String packageName) {
        return mGcm.contains(packageName);
    }

    public boolean isFavorite(String packageName) {
        return mFavorite.containsKey(packageName);
    }

    public boolean isLauncher(String packageName) {
        return mLauncher != null && mLauncher.equals(packageName);
    }

    public boolean isGms(String packageName) {
        if (GMS.equals(packageName)) {
            Integer important = mFavorite.get(packageName);
            return important != null && important == IMPORTANT_GMS;
        } else {
            return false;
        }
    }

    private boolean updateBrevent(boolean brevent) {
        AppsFragment fragment = getFragment();
        Collection<String> selected = new ArraySet<>(fragment.getSelected());
        if (brevent) {
            Iterator it = selected.iterator();
            while (it.hasNext()) {
                if (mImportant.containsKey(it.next())) {
                    it.remove();
                }
            }
        }
        if (!selected.isEmpty()) {
            BreventPackages breventPackages = new BreventPackages(brevent, selected);
            breventPackages.undoable = true;
            mHandler.obtainMessage(MESSAGE_BREVENT_REQUEST, breventPackages).sendToTarget();
        }
        clearSelected();
        return false;
    }

    private AppsFragment getFragment() {
        return mAdapter.getFragment(mPager.getCurrentItem());
    }

    private void showHome(boolean show) {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(show);
        }
    }

    private boolean onClickHome() {
        if (mSelectStatus != 0) {
            clearSelected();
        } else if (resetSearchView()) {
            invalidateOptionsMenu();
        }
        return true;
    }

    private void clearSelected() {
        AppsFragment fragment = getFragment();
        fragment.clearSelected();
        setSelectCount(0);
    }

    private int getSelectStatus() {
        int status = 0;
        for (String packageName : getFragment().getSelected()) {
            if (mBrevent.contains(packageName)) {
                status |= STATUS_BREVENT;
            } else if (mImportant.containsKey(packageName)) {
                status |= STATUS_IMPORTANT;
            } else {
                status |= STATUS_UNBREVENT;
            }
            if ((status & STATUS_MASK) == STATUS_MASK) {
                break;
            }
        }
        return status;
    }

    public void setSelectCount(int count) {
        int selectStatus = getSelectStatus();
        if (mSelectStatus != selectStatus) {
            invalidateOptionsMenu();
            showHome(selectStatus != 0);
            mSelectStatus = selectStatus;
        }
        if (selectStatus != 0) {
            if (mSnackBar != null && mSnackBar.isShown()) {
                mSnackBar.dismiss();
                mSnackBar = null;
            }
            mToolbar.setTitle(String.valueOf(count));
        } else {
            mToolbar.setTitle(R.string.brevent);
        }
    }

    public void onBreventResponse(@Nullable BreventProtocol response) {
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("response: " + response);
        }
        if (response == null || response == BreventOK.INSTANCE) {
            uiHandler.sendEmptyMessage(UI_MESSAGE_NO_PERMISSION);
        } else {
            dispatchResponse(response);
        }
    }

    private void dispatchResponse(@NonNull BreventProtocol response) {
        int action = response.getAction();
        if (action != BreventProtocol.STATUS_NO_EVENT) {
            collapsePanels();
        }
        switch (action) {
            case BreventProtocol.STATUS_RESPONSE:
                uiHandler.removeMessages(UI_MESSAGE_MAKE_EVENT);
                BreventApplication application = (BreventApplication) getApplication();
                application.resetEvent();
                onBreventStatusResponse((BreventResponse) response);
                break;
            case BreventProtocol.UPDATE_BREVENT:
                onBreventPackagesResponse((BreventPackages) response);
                break;
            case BreventProtocol.UPDATE_PRIORITY:
                onBreventPriorityResponse((BreventPriority) response);
                break;
            case BreventProtocol.STATUS_NO_EVENT:
                onBreventNoEvent((BreventNoEvent) response);
                break;
            default:
                break;
        }
    }

    private void onBreventNoEvent(BreventNoEvent response) {
        if (response.versionMismatched()) {
            showUnsupported(R.string.unsupported_version_mismatched, true);
        } else if (response.mExit) {
            showUnsupported(R.string.unsupported_no_event, true);
        } else {
            uiHandler.sendEmptyMessage(UI_MESSAGE_NO_EVENT);
            mHandler.sendEmptyMessageDelayed(MESSAGE_RETRIEVE2, DELAY);
            expandNotificationsPanel();
            BreventApplication application = (BreventApplication) getApplication();
            if (!application.isEventMade()) {
                uiHandler.sendEmptyMessageDelayed(UI_MESSAGE_MAKE_EVENT, DELAY5);
            }
        }
    }

    private void expandNotificationsPanel() {
        if (!notificationEventMade) {
            try {
                IStatusBarService service = IStatusBarService.Stub
                        .asInterface(ServiceManager.getService("statusbar"));
                service.expandNotificationsPanel();
                notificationEventMade = true;
            } catch (RemoteException | RuntimeException | LinkageError e) {
                UILog.d("Can't expandNotificationsPanel: " + e.getMessage(), e);
            }
        }
    }

    private void collapsePanels() {
        if (notificationEventMade) {
            try {
                IStatusBarService service = IStatusBarService.Stub
                        .asInterface(ServiceManager.getService("statusbar"));
                service.collapsePanels();
                notificationEventMade = false;
            } catch (RemoteException | RuntimeException | LinkageError e) {
                UILog.d("Can't collapsePanels: " + e.getMessage(), e);
            }
        }
    }

    private void onBreventPackagesResponse(BreventPackages response) {
        if (!response.packageNames.isEmpty()) {
            AppsSnackbarCallback callback = new AppsSnackbarCallback(mHandler, uiHandler, response);
            Snackbar snackbar;
            if (response.undoable) {
                String message = getString(response.brevent ? R.string.action_brevent_results :
                        R.string.action_restore_results, response.packageNames.size());
                snackbar = Snackbar.make(mCoordinator, message, BaseTransientBottomBar.LENGTH_LONG);
                snackbar.setAction(R.string.action_message_undo, callback);
            } else {
                String message = getString(R.string.action_message_undone);
                snackbar = Snackbar.make(mCoordinator, message, BaseTransientBottomBar.LENGTH_LONG);
            }
            snackbar.addCallback(callback);
            snackbar.show();
        }
    }

    public void updateBreventResponse(BreventPriority breventPriority) {
        if (breventPriority.priority) {
            mPriority.addAll(breventPriority.packageNames);
        } else {
            mPriority.removeAll(breventPriority.packageNames);
        }
        if (mAdapter != null) {
            AppsFragment fragment = getFragment();
            fragment.update(breventPriority.packageNames);
        }
    }

    private void onBreventPriorityResponse(BreventPriority response) {
        if (!response.packageNames.isEmpty()) {
            uiHandler.obtainMessage(UI_MESSAGE_UPDATE_PRIORITY, response).sendToTarget();
        }
    }

    private void onBreventStatusResponse(BreventResponse status) {
        BreventApplication application = (BreventApplication) getApplication();
        application.updateStatus(status);
        showAlipay(status.mAlipaySum);
        if (shouldOpenSettings) {
            shouldOpenSettings = false;
            openSettings();
            return;
        }

        if (mStats == null) {
            synchronized (updateLock) {
                if (mStats == null) {
                    mStats = new SimpleArrayMap<>();
                    retrieveStats();
                }
            }
        }

        synchronized (updateLock) {
            mProcesses.clear();
            mProcesses.putAll(status.mProcesses);
        }

        mBrevent.clear();
        mBrevent.addAll(status.mBrevent);

        mPriority.clear();
        mPriority.addAll(status.mPriority);

        mImportant.clear();
        mFavorite.clear();
        mGcm.clear();
        resolveImportantPackages(status.mProcesses);
        for (String packageName : status.mTrustAgents) {
            mImportant.put(packageName, IMPORTANT_TRUST_AGENT);
        }
        for (String packageName : status.mAndroidProcesses) {
            mImportant.put(packageName, IMPORTANT_ANDROID);
        }
        for (String packageName : status.mFullPowerList) {
            mFavorite.put(packageName, IMPORTANT_BATTERY);
        }
        mVpn = status.mVpn;
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("favorite: " + mFavorite);
            UILog.d("vpn: " + mVpn);
        }

        if (hasGms()) {
            mFavorite.put(GMS, IMPORTANT_GMS);
            if (((BreventApplication) getApplication()).supportStopped()) {
                resolveGcmPackages(mGcm);
            }
        }

        if (mAdapter == null) {
            mAdapter = new AppsPagerAdapter(getFragmentManager(), mTitles);
            mPackages.clear();
            mPackages.addAll(status.mPackages);
        } else if (!Objects.equals(mPackages, status.mPackages)) {
            mPackages.clear();
            mPackages.addAll(status.mPackages);
            mAdapter.setExpired();
        }
        if (uiHandler == null) {
            return;
        }
        uiHandler.sendEmptyMessage(UI_MESSAGE_HIDE_PROGRESS);
        uiHandler.sendEmptyMessage(UI_MESSAGE_SHOW_PAGER);

        if (!hasResponse) {
            doUpdateConfiguration();
            unregisterReceiver();
            hideReport();
            hasResponse = true;
        }

        if (mSelectStatus == 0 && mBrevent.isEmpty()) {
            uiHandler.sendEmptyMessage(UI_MESSAGE_SHOW_SUCCESS);
        }
        unbreventImportant();
        if (isCheck()) {
            setAlarm(this);
        } else {
            cancelAlarm(this);
        }

        if (application.isPlay() && BuildConfig.RELEASE && !mBrevent.isEmpty()) {
            int days = 0;
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, 0);
                long duration = System.currentTimeMillis() - pi.firstInstallTime;
                days = (int) TimeUnit.MILLISECONDS.toDays(duration);
            } catch (PackageManager.NameNotFoundException ignore) {
                //
            }
            if (days > 0x2) {
                checkBreventList(application, days);
            }
        }
    }

    private void checkBreventList(BreventApplication application, int days) {
        int donated = application.getDonated();
        int size = mBrevent.size();
        int required = BreventSettings.getRecommend(this, size);
        if (required > donated) {
            SharedPreferences sp = PreferencesUtils.getPreferences(this);
            long daemonTime = sp.getLong(BreventSettings.DAEMON_TIME, 0);
            int mbRequired = sp.getInt(AppsPaymentFragment.REQUIRED, 0);
            int mbDays = sp.getInt(AppsPaymentFragment.DAYS, 0);
            if (daemonTime != application.mDaemonTime || mbRequired != required || mbDays != days) {
                showPayment(days, size, required);
                sp.edit().putLong(BreventSettings.DAEMON_TIME, application.mDaemonTime)
                        .putInt(AppsPaymentFragment.DAYS, days)
                        .putInt(AppsPaymentFragment.REQUIRED, required).apply();
            }
        }
    }

    private void showAlipay(String alipaySum) {
        if (alipaySum != null) {
            BreventServerReceiver.showAlipay(((BreventApplication) getApplication()), alipaySum);
            doUpdateConfiguration();
        }
    }

    private void retrieveStats() {
        UsageStatsManager manager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST,
                BEGIN, System.currentTimeMillis());
        if (stats == null) {
            UILog.d("no stats");
            return;
        }
        for (UsageStats stat : stats) {
            String packageName = stat.getPackageName();
            int key = mStats.indexOfKey(packageName);
            if (key >= 0) {
                mStats.valueAt(key).add(stat);
            } else {
                mStats.put(packageName, new UsageStats(stat));
            }
        }
    }

    private void unbreventImportant() {
        Set<String> importantBrevented = new ArraySet<>();
        for (String packageName : mBrevent) {
            if (mImportant.containsKey(packageName)) {
                importantBrevented.add(packageName);
            }
        }
        if (!importantBrevented.isEmpty()) {
            UILog.i("will unbrevent: " + importantBrevented);
            BreventPackages breventPackages = new BreventPackages(false, importantBrevented);
            breventPackages.undoable = false;
            mHandler.obtainMessage(MESSAGE_BREVENT_REQUEST, breventPackages).sendToTarget();
        }
        makePriority();
    }

    private void makePriority() {
        if (mSms != null) {
            updatePriority(mSms);
        }
        if (mDialer != null) {
            updatePriority(mDialer);
        }
        updatePriority(BuildConfig.APPLICATION_ID);
    }

    private void updatePriority(String packageName) {
        if (mBrevent.contains(packageName) && !mPriority.contains(packageName)) {
            updatePriority(packageName, true);
        }
    }

    private Collection<String> checkReceiver(Intent intent) {
        Set<String> packageNames = new ArraySet<>();
        List<ResolveInfo> resolveInfos = getPackageManager().queryBroadcastReceivers(intent, 0);
        if (resolveInfos != null) {
            for (ResolveInfo resolveInfo : resolveInfos) {
                packageNames.add(resolveInfo.activityInfo.packageName);
            }
        }
        return packageNames;
    }

    private String getSecureSetting(String name) {
        return Settings.Secure.getString(getContentResolver(), name);
    }

    private void resolveImportantPackages(SimpleArrayMap<String, SparseIntArray> processes) {
        // input method
        String input = getPackageName(getSecureSetting(Settings.Secure.DEFAULT_INPUT_METHOD));
        if (input != null) {
            mImportant.put(input, IMPORTANT_INPUT);
        }

        // next alarm
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo alarmClock = alarmManager.getNextAlarmClock();
        if (alarmClock != null && alarmClock.getShowIntent() != null) {
            String alarmClockPackage = alarmClock.getShowIntent().getCreatorPackage();
            mImportant.put(alarmClockPackage, IMPORTANT_ALARM);
        }

        // sms
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            mSms = getSecureSetting(HideApiOverride.SMS_DEFAULT_APPLICATION);
            if (isSystemPackage(mSms)) {
                mFavorite.put(mSms, IMPORTANT_SMS);
            } else {
                mImportant.put(mSms, IMPORTANT_SMS);
            }

            // dialer
            mDialer = getDefaultApp(Intent.ACTION_DIAL);
            if (mDialer != null) {
                if (isSystemPackage(mDialer)) {
                    mFavorite.put(mDialer, IMPORTANT_DIALER);
                } else {
                    mImportant.put(mDialer, IMPORTANT_DIALER);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mDialer = ((TelecomManager) getSystemService(TELECOM_SERVICE))
                        .getDefaultDialerPackage();
                if (mDialer != null) {
                    if (isSystemPackage(mDialer)) {
                        mFavorite.put(mDialer, IMPORTANT_DIALER);
                    } else {
                        mImportant.put(mDialer, IMPORTANT_DIALER);
                    }
                }
            }
        } else {
            mSms = null;
            mDialer = null;
        }

        // assistant
        String assistant;
        assistant = getPackageName(getSecureSetting(HideApiOverride.getVoiceInteractionService()));
        if (assistant != null) {
            mImportant.put(assistant, IMPORTANT_ASSISTANT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assistant = getPackageName(getSecureSetting(HideApiOverride.getAssistant()));
            if (assistant != null) {
                mImportant.put(assistant, IMPORTANT_ASSISTANT);
            }
        }

        // webview
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String webView = Settings.Global.getString(getContentResolver(),
                    HideApiOverrideN.WEBVIEW_PROVIDER);
            mImportant.put(webView, IMPORTANT_WEBVIEW);
        }

        // launcher
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null) {
            mLauncher = resolveInfo.activityInfo.packageName;
            mImportant.put(mLauncher, IMPORTANT_HOME);
        }

        // installer
        intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setDataAndType(Uri.fromFile(new File("foo.apk")), PACKAGE_MIME_TYPE);
        resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null) {
            mImportant.put(resolveInfo.activityInfo.packageName, IMPORTANT_ANDROID);
        }
        mImportant.put("com.android.defcontainer", IMPORTANT_ANDROID);

        AccessibilityManager accessibility = (AccessibilityManager) getSystemService(
                Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabled = accessibility.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo accessibilityServiceInfo : enabled) {
            mImportant.put(accessibilityServiceInfo.getResolveInfo().serviceInfo.packageName,
                    IMPORTANT_ACCESSIBILITY);
        }

        DevicePolicyManager devicePolicy = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        List<ComponentName> componentNames = devicePolicy.getActiveAdmins();
        if (componentNames != null) {
            for (ComponentName componentName : componentNames) {
                mImportant.put(componentName.getPackageName(), IMPORTANT_DEVICE_ADMIN);
            }
        }

        // persistent
        int size = processes.size();
        for (int i = 0; i < size; ++i) {
            if (BreventResponse.isPersistent(processes.valueAt(i))) {
                mImportant.put(processes.keyAt(i), IMPORTANT_PERSISTENT);
            }
        }

        // wallpaper
        WallpaperInfo wallpaperInfo = WallpaperManager.getInstance(this).getWallpaperInfo();
        if (wallpaperInfo != null) {
            mImportant.put(wallpaperInfo.getPackageName(), IMPORTANT_WALLPAPER);
        }

        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("important: " + mImportant);
        }
    }

    private String getDefaultApp(String action) {
        Intent intent = new Intent(action);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null) {
            return resolveInfo.activityInfo.packageName;
        } else {
            return null;
        }
    }

    private String getPackageName(String component) {
        if (component != null) {
            ComponentName componentName = ComponentName.unflattenFromString(component);
            if (componentName != null) {
                return componentName.getPackageName();
            }
        }
        return "";
    }

    private void resolveGcmPackages(Set<String> packageNames) {
        // gcm
        List<PackageInfo> packageInfos = getPackageManager().getPackagesHoldingPermissions(
                new String[] {"com.google.android.c2dm.permission.RECEIVE"}, 0);
        if (packageInfos != null) {
            Set<String> gcm = new ArraySet<>();
            for (PackageInfo packageInfo : packageInfos) {
                gcm.add(packageInfo.packageName);
            }
            gcm.retainAll(checkReceiver(new Intent("com.google.android.c2dm.intent.RECEIVE")));
            packageNames.addAll(gcm);
        }
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("gcm: " + packageNames);
        }
    }

    public void showViewPager() {
        dismissDialog(FRAGMENT_UNSUPPORTED, false);
        mPager.setVisibility(View.VISIBLE);
        updateAdapter(mAdapter);
        if (mPager.getAdapter() == null) {
            mPager.setAdapter(mAdapter);
        } else {
            mAdapter.refreshFragment();
        }
    }

    public void setRefreshEnabled(boolean enabled) {
        if (mAdapter != null) {
            mAdapter.setRefreshEnabled(enabled);
        }
    }

    private boolean updateAdapter(AppsPagerAdapter adapter) {
        SharedPreferences sp = PreferencesUtils.getPreferences(this);
        boolean showAllApps = sp.getBoolean(SettingsFragment.SHOW_ALL_APPS,
                SettingsFragment.DEFAULT_SHOW_ALL_APPS);
        boolean showFramework = sp.getBoolean(SettingsFragment.SHOW_FRAMEWORK_APPS,
                SettingsFragment.DEFAULT_SHOW_FRAMEWORK_APPS) || breventedFrameworkApps();
        return adapter.setShowAllApps(showAllApps) | adapter.setShowFramework(showFramework);
    }

    private boolean breventedFrameworkApps() {
        PackageManager packageManager = getPackageManager();
        for (String packageName : mBrevent) {
            if (isFrameworkPackage(packageManager, packageName)) {
                return true;
            }
        }
        return false;
    }

    public void showFragmentAsync(AppsFragment fragment, long delayMillis) {
        Message message = uiHandler.obtainMessage(UI_MESSAGE_SHOW_FRAGMENT, fragment);
        uiHandler.sendMessageDelayed(message, delayMillis);
    }

    public void updateBreventResponse(BreventPackages breventPackages) {
        if (breventPackages.brevent) {
            mBrevent.addAll(breventPackages.packageNames);
            makePriority();
        } else {
            mBrevent.removeAll(breventPackages.packageNames);
        }
        if (mAdapter != null) {
            AppsFragment fragment = getFragment();
            if (!breventPackages.undoable) {
                fragment.select(breventPackages.packageNames);
            } else {
                fragment.update(breventPackages.packageNames);
            }
        }
    }

    public void copy(String content) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, content));
    }

    public void showMessage(String message) {
        Snackbar.make(mCoordinator, message, BaseTransientBottomBar.LENGTH_LONG).show();
    }

    public String getLabel(String label, String packageName) {
        int index = mImportant.indexOfKey(packageName);
        int important = -1;
        if (index >= 0) {
            important = mImportant.valueAt(index);
        } else {
            index = mFavorite.indexOfKey(packageName);
            if (index >= 0) {
                important = mFavorite.valueAt(index);
            }
        }
        switch (important) {
            case IMPORTANT_INPUT:
                return getString(R.string.important_input, label);
            case IMPORTANT_ALARM:
                return getString(R.string.important_alarm, label);
            case IMPORTANT_SMS:
                return getString(R.string.important_sms, label);
            case IMPORTANT_HOME:
                return getString(R.string.important_home, label);
            case IMPORTANT_PERSISTENT:
                return getString(R.string.important_persistent, label);
            case IMPORTANT_ANDROID:
                return getString(R.string.important_android, label);
            case IMPORTANT_DIALER:
                return getString(R.string.important_dialer, label);
            case IMPORTANT_ASSISTANT:
                return getString(R.string.important_assistant, label);
            case IMPORTANT_WEBVIEW:
                return getString(R.string.important_webview, label);
            case IMPORTANT_ACCESSIBILITY:
                return getString(R.string.important_accessibility, label);
            case IMPORTANT_DEVICE_ADMIN:
                return getString(R.string.important_device_admin, label);
            case IMPORTANT_BATTERY:
                return getString(R.string.important_battery, label);
            case IMPORTANT_TRUST_AGENT:
                return getString(R.string.important_trust_agent, label);
            case IMPORTANT_GMS:
                return getString(R.string.important_gms, label);
            case IMPORTANT_WALLPAPER:
                return getString(R.string.important_wallpaper, label);
            default:
                break;
        }
        return label;
    }

    public void runAsRoot() {
        BreventApplication application = (BreventApplication) getApplication();
        application.setHandler(mHandler);
        showProgress(R.string.process_starting);
        mHandler.removeMessages(MESSAGE_RETRIEVE2);
        mHandler.sendEmptyMessageDelayed(MESSAGE_RETRIEVE2, DELAY5);
        uiHandler.sendEmptyMessageDelayed(BreventActivity.UI_MESSAGE_NO_BREVENT, DELAY15);
        BreventIntentService.startBrevent((BreventApplication) getApplication(),
                BreventIntent.ACTION_RUN_AS_ROOT);
    }

    public void updatePriority(String packageName, boolean priority) {
        BreventProtocol request = new BreventPriority(priority, Collections.singleton(packageName));
        mHandler.obtainMessage(MESSAGE_BREVENT_REQUEST, request).sendToTarget();
    }

    public boolean isBrevent(String packageName) {
        return mBrevent.contains(packageName);
    }

    public void showSuccess() {
        mSnackBar = Snackbar.make(mCoordinator, R.string.brevent_service_success,
                BaseTransientBottomBar.LENGTH_INDEFINITE);
        mSnackBar.show();
    }

    public static Signature[] getSignatures(PackageManager packageManager, String packageName) {
        try {
            return getSignatures(packageManager.getApplicationInfo(packageName, 0).sourceDir);
        } catch (PackageManager.NameNotFoundException e) {
            // do nothing
            return null;
        }
    }

    public static Signature[] getSignatures(String sourceDir) {
        try {
            PackageParser.Package pkg = new PackageParser.Package(sourceDir);
            pkg.baseCodePath = sourceDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageParser.collectCertificates(pkg, PackageManager.GET_SIGNATURES);
            } else {
                HideApiOverrideM.collectCertificates(pkg, PackageManager.GET_SIGNATURES);
            }
            return pkg.mSignatures;
        } catch (PackageParser.PackageParserException e) {
            // do nothing
            return null;
        }
    }

    private void unregisterReceiver() {
        if (mConnectedReceiver != null) {
            unregisterReceiver(mConnectedReceiver);
            mConnectedReceiver = null;
        }
    }

    void updateDisabled(boolean connected) {
        AppsDisabledFragment fragment = (AppsDisabledFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_DISABLED);
        if (fragment != null && connected != fragment.isConnected()) {
            showDisabled(fragment.getTitle(), true);
        }
    }

    private boolean isSystemPackage(String packageName) {
        try {
            return isSystemPackage(getPackageManager().getApplicationInfo(packageName, 0).flags);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static boolean isSystemPackage(int flags) {
        return (flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    @SuppressLint("WrongConstant")
    public void makeEvent() {
        ((BreventApplication) getApplication()).makeEvent();
        UILog.d("make event by restart");
        // https://stackoverflow.com/a/3419987/3289354
        // recreate has no appropriate event
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        super.finish();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    public void showNoPermission() {
        hideProgress();
        hideDisabled();
        showUnsupported(R.string.unsupported_permission);
    }

    public void showNoEvent() {
        hideDisabled();
        showProgress(R.string.process_waiting);
    }

    public void onLogsCompleted(File path) {
        if (path == null) {
            showUnsupported(R.string.unsupported_logs);
        } else {
            sendEmail(this, path, getString(R.string.logs_description, Build.FINGERPRINT));
        }
    }

    static Intent getEmailIntent() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("mailto:"));
        return intent;
    }

    static boolean hasEmailClient(Context context) {
        return context.getPackageManager().resolveActivity(getEmailIntent(),
                PackageManager.MATCH_DEFAULT_ONLY) != null;
    }

    private static String getSubject(Context context) {
        return context.getString(R.string.brevent) + " " + BuildConfig.VERSION_NAME +
                "(Android " + LocaleUtils.getOverrideLocale(context)
                + "-" + Build.VERSION.RELEASE + ")";
    }

    public static void sendEmail(Context context, File path, String content) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("message/rfc822");
        if (path != null) {
            try {
                Uri uri = FileProvider.getUriForFile(context,
                        BuildConfig.APPLICATION_ID + ".fileprovider", path);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
            } catch (IllegalArgumentException e) {
                UILog.w("Can't get uri for " + path);
            }
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, getSubject(context));
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {BuildConfig.EMAIL});
        sendEmail(context, intent);
    }

    public static void sendEmail(Context context, Intent intent) {
        Intent email = getEmailIntent();
        PackageManager packageManager = LocaleUtils.getSystemContext(context).getPackageManager();
        Set<ComponentName> emails = new ArraySet<>();
        for (ResolveInfo resolveInfo : packageManager.queryIntentActivities(email, 0)) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            emails.add(new ComponentName(activityInfo.packageName, activityInfo.name));
        }
        List<ResolveInfo> rfc822 = packageManager.queryIntentActivities(intent, 0);
        List<Intent> intents = new ArrayList<>();
        for (ResolveInfo resolveInfo : rfc822) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            ComponentName componentName = new ComponentName(activityInfo.packageName,
                    activityInfo.name);
            if (emails.contains(componentName)) {
                Intent original = new Intent(intent).setComponent(componentName);
                CharSequence activityLabel = resolveInfo.loadLabel(packageManager);
                CharSequence appLabel = activityInfo.applicationInfo.loadLabel(packageManager);
                CharSequence label;
                if (Objects.equals(activityLabel, appLabel)) {
                    label = activityLabel;
                } else {
                    label = context.getString(R.string.email_label, activityLabel, appLabel);
                }
                Intent labeled = new LabeledIntent(original, activityInfo.packageName,
                        label, resolveInfo.icon);
                intents.add(labeled);
            }
        }
        CharSequence title = context.getText(R.string.feedback_email);
        if (intents.isEmpty()) {
            context.startActivity(Intent.createChooser(intent, title));
        } else {
            Intent chooser = Intent.createChooser(intents.remove(0), title);
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                    intents.toArray(new Parcelable[intents.size()]));
            context.startActivity(chooser);
        }
    }

    @Override
    public void onRefresh() {
        if (mHandler != null) {
            mHandler.sendEmptyMessage(MESSAGE_RETRIEVE2);
        }
    }

    public void showRootCompleted(List<String> output) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(Build.FINGERPRINT);
        pw.println();
        for (String s : output) {
            pw.println(s);
        }
        showShellCompleted(sw.toString());
    }

    public boolean showShellCompleted(String message) {
        if (hasResponse) {
            return true;
        }
        BreventApplication application = (BreventApplication) getApplication();
        if (application.isStarted() || checkPort(application)) {
            mHandler.sendEmptyMessage(MESSAGE_RETRIEVE2);
            return true;
        }
        hideDisabled();
        hideProgress();
        ReportFragment fragment = new ReportFragment();
        fragment.setDetails(R.string.unsupported_shell, message);
        fragment.show(getFragmentManager(), FRAGMENT_REPORT);
        mHandler.removeCallbacksAndMessages(null);
        uiHandler.removeCallbacksAndMessages(null);
        return false;
    }

    private boolean checkPort(BreventApplication application) {
        try {
            return application.checkPort(false);
        } catch (NetworkErrorException ignore) { // NOSONAR
            // do nothing
            return false;
        }
    }

    public String getQuery() {
        return mQuery;
    }

    @Override
    public void onClick(View v) {
        if (v instanceof SearchView) {
            showHome(true);
        }
    }

    @Override
    public boolean onClose() {
        showHome(false);
        updateQuery(null);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchView.clearFocus();
        updateQuery(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    private void updateQuery(String query) {
        mQuery = query;
        if (mAdapter != null) {
            mAdapter.setExpired();
        }
    }

    private boolean resetSearchView() {
        if (mSearchView != null && !mSearchView.isIconified()) {
            if (mQuery != null) {
                updateQuery(null);
            }
            mSearchView.setIconified(true);
            showHome(false);
            invalidateOptionsMenu();
            return true;
        } else {
            return false;
        }
    }

    private boolean updateCheck() {
        SharedPreferences preferences = PreferencesUtils.getPreferences(this);
        if (mConfiguration == null) {
            mConfiguration = new BreventConfiguration(preferences);
        }
        boolean checking = preferences.getBoolean("brevent_checking", false);
        if (check == null || check != checking) {
            check = checking;
            return true;
        } else {
            return false;
        }
    }

    public boolean isCheck() {
        if (check == null) {
            updateCheck();
        }
        return check;
    }

    public UsageStats getStats(String packageName) {
        return mStats == null ? null : mStats.get(packageName);
    }

    public boolean hasStats() {
        return mStats != null && !mStats.isEmpty();
    }

    public boolean hasOps(String packageName) {
        return getPackageManager().checkPermission("android.permission.GET_APP_OPS_STATS",
                BuildConfig.APPLICATION_ID) == PackageManager.PERMISSION_GRANTED
                && OpsItemAdapter.getOpsForPackage(packageName) != null;
    }

    public void updateSort() {
        if (mAdapter != null) {
            mAdapter.setExpired();
        }
    }

    private boolean isBreventFramework() {
        PackageManager packageManager = getPackageManager();
        return packageManager.checkSignatures(PACKAGE_FRAMEWORK, BuildConfig.APPLICATION_ID) ==
                PackageManager.SIGNATURE_MATCH;
    }

    private boolean isFrameworkPackage(PackageManager packageManager, String packageName) {
        if (fakeFramework) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                return isFrameworkPackage(packageManager, packageInfo);
            } catch (PackageManager.NameNotFoundException e) {
                UILog.d("cannot find " + packageName, e);
                return false;
            }
        } else {
            return packageManager.checkSignatures(PACKAGE_FRAMEWORK, packageName) ==
                    PackageManager.SIGNATURE_MATCH;
        }
    }

    boolean isFrameworkPackage(PackageManager packageManager, PackageInfo packageInfo) {
        String packageName = packageInfo.packageName;
        if (fakeFramework) {
            SharedPreferences preferences = getSharedPreferences("signature", Context.MODE_PRIVATE);
            long lastSync = AppsLabelLoader.getLastSync(this);
            if (preferences.contains(packageName) && packageInfo.lastUpdateTime <= lastSync) {
                return preferences.getBoolean(packageName, false);
            }
            boolean isFrameworkPackage = isFrameworkPackageSignature(packageManager, packageName);
            preferences.edit().putBoolean(packageName, isFrameworkPackage).apply();
            return isFrameworkPackage;
        } else {
            return packageManager.checkSignatures(PACKAGE_FRAMEWORK, packageName) ==
                    PackageManager.SIGNATURE_MATCH;
        }
    }

    private boolean isFrameworkPackageSignature(PackageManager packageManager, String packageName) {
        boolean frameworkApp = Arrays.equals(getFrameworkSignatures(packageManager),
                BreventActivity.getSignatures(packageManager, packageName));
        UILog.i("checking framework app for " + packageName + ": " + (frameworkApp ? "yes" : "no"));
        return frameworkApp;
    }

    private Signature[] getFrameworkSignatures(PackageManager packageManager) {
        if (frameworkSignatures == null) {
            frameworkSignatures = BreventActivity.getSignatures(packageManager, PACKAGE_FRAMEWORK);
        }
        return frameworkSignatures;
    }

    private static class UsbConnectedReceiver extends BroadcastReceiver {

        private final BreventActivity mActivity;

        public UsbConnectedReceiver(BreventActivity activity) {
            mActivity = activity;
        }

        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (HideApiOverride.ACTION_USB_STATE.equals(action)) {
                boolean connected = intent.getBooleanExtra(HideApiOverride.USB_CONNECTED, false);
                mActivity.updateDisabled(connected);
            }
        }
    }

}
