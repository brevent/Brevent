package me.piebridge.brevent.ui;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
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
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toolbar;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dalvik.system.PathClassLoader;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.override.HideApiOverride;
import me.piebridge.brevent.override.HideApiOverrideM;
import me.piebridge.brevent.override.HideApiOverrideN;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventNoEvent;
import me.piebridge.brevent.protocol.BreventPackages;
import me.piebridge.brevent.protocol.BreventPriority;
import me.piebridge.brevent.protocol.BreventProtocol;
import me.piebridge.brevent.protocol.BreventResponse;

public class BreventActivity extends Activity
        implements ViewPager.OnPageChangeListener, SwipeRefreshLayout.OnRefreshListener {

    private static final int DELAY = 1000;

    private static final int DELAY5 = 5000;

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

    private static final int REQUEST_CODE_SETTINGS = 1;

    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";

    private ViewPager mPager;

    private SwipeRefreshLayout mRefresh;

    private AppsPagerAdapter mAdapter;

    private String[] mTitles;

    private SimpleArrayMap<String, SparseIntArray> mProcesses = new SimpleArrayMap<>();
    private Set<String> mBrevent = new ArraySet<>();
    private Set<String> mPriority = new ArraySet<>();
    private SimpleArrayMap<String, Integer> mImportant = new SimpleArrayMap<>();
    private SimpleArrayMap<String, Integer> mFavorite = new SimpleArrayMap<>();
    private Set<String> mGcm = new ArraySet<>();

    private boolean mSelectMode;

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

    private volatile boolean stopped;

    private volatile boolean hasResponse;

    private int mInstalledCount;

    private UsbConnectedReceiver mConnectedReceiver;

    private final Object updateLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            } catch (Throwable t) {
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
        if (!disabledXposed) {
            showUnsupported(R.string.unsupported_xposed);
        } else if (!BreventApplication.IS_OWNER) {
            showUnsupported(R.string.unsupported_owner);
        } else if (!verifySignature()) {
            showUnsupported(R.string.unsupported_signature);
        } else if (isFlymeClone()) {
            showUnsupported(R.string.unsupported_clone);
        } else if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(BreventGuide.GUIDE, true)) {
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

            mColorControlNormal = ColorUtils.resolveColor(this, android.R.attr.colorControlNormal);
            mTextColorPrimary = ColorUtils.resolveColor(this, android.R.attr.textColorPrimary);
            mColorControlHighlight = ColorUtils.resolveColor(this,
                    android.R.attr.colorControlHighlight);

            uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_SHOW_PROGRESS);
        }
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

    private void showUnsupported(int resId) {
        UnsupportedFragment fragment = new UnsupportedFragment();
        fragment.setMessage(resId);
        fragment.show(getFragmentManager(), FRAGMENT_UNSUPPORTED);
    }

    private boolean verifySignature() {
        if (!BuildConfig.RELEASE) {
            return true;
        }
        Signature[] signatures = getSignatures(getPackageManager(), BuildConfig.APPLICATION_ID);
        return signatures != null && signatures.length == 0x1 &&
                Arrays.equals(BuildConfig.SIGNATURE, sha1(signatures[0].toByteArray()));
    }

    public boolean hasGms() {
        PackageManager packageManager = getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(GMS, 0);
            if (!packageInfo.applicationInfo.enabled) {
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
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
        Signature[] signatures = getSignatures(sourceDir);
        if (signatures == null || signatures.length != 0x1) {
            return false;
        }
        byte[] sha1 = sha1(signatures[0].toByteArray());
        for (byte[] bytes : GMS_SIGNATURES) {
            if (Arrays.equals(sha1, bytes)) {
                return true;
            }
        }
        return false;
    }

    public static byte[] sha1(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(bytes);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            UILog.w("NoSuchAlgorithmException", e);
            return null;
        }
    }

    public void showDisabled() {
        hideProgress();
        showDisabled(R.string.brevent_service_start);
    }

    public void showDisabled(int title) {
        showDisabled(title, false);
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
            fragment.update(title);
            fragment.show(getFragmentManager(), FRAGMENT_DISABLED);
        }
        if (hasResponse) {
            mHandler.sendEmptyMessage(MESSAGE_RETRIEVE);
        }
    }

    public void hideDisabled() {
        dismissDialog(FRAGMENT_DISABLED, false);
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
            fragment.updateMessage(message);
            fragment.show(getFragmentManager(), FRAGMENT_PROGRESS);
        }
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
        stopped = true;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            uiHandler.removeCallbacksAndMessages(null);
        }
        unregisterReceiver();
        super.onStop();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        stopped = false;
        if (mHandler != null) {
            if (((BreventApplication) getApplication()).isRunningAsRoot()) {
                mHandler.sendEmptyMessage(MESSAGE_RETRIEVE2);
            } else {
                mHandler.sendEmptyMessage(MESSAGE_RETRIEVE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        stopped = true;
        super.onSaveInstanceState(outState);
    }

    private void dismissDialog() {
        dismissDialog(FRAGMENT_DISABLED, true);
        dismissDialog(FRAGMENT_PROGRESS, true);
        dismissDialog(FRAGMENT_UNSUPPORTED, true);
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
        SparseIntArray status = mProcesses.get(packageName);
        if (status == null) {
            return AppsInfo.STATUS_STOPPED;
        } else if (BreventResponse.isStandby(status)) {
            return AppsInfo.STATUS_STANDBY;
        } else {
            return AppsInfo.STATUS_RUNNING;
        }
    }

    public String getDescription(String packageName) {
        SparseIntArray status = mProcesses.get(packageName);
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
        SparseIntArray status = mProcesses.get(packageName);
        if (isFavorite(packageName)) {
            return R.drawable.ic_favorite_border_black_24dp;
        } else if (status != null && !BreventResponse.isService(status)) {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        if (!mSelectMode) {
            if (BuildConfig.RELEASE) {
                menu.add(Menu.NONE, R.string.menu_feedback, Menu.NONE, R.string.menu_feedback);
                if (canFetchLogs()) {
                    menu.add(Menu.NONE, R.string.menu_logs, Menu.NONE, R.string.menu_logs);
                }
            }
            menu.add(Menu.NONE, R.string.menu_guide, Menu.NONE, R.string.menu_guide);
            menu.add(Menu.NONE, R.string.menu_settings, Menu.NONE, R.string.menu_settings);
        } else {
            MenuItem remove = menu.add(Menu.NONE, R.string.action_restore, Menu.NONE,
                    R.string.action_restore).setIcon(R.drawable.ic_panorama_fish_eye_black_24dp);
            remove.getIcon().setTint(mColorControlNormal);
            remove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            MenuItem brevent = menu.add(Menu.NONE, R.string.action_brevent, Menu.NONE,
                    R.string.action_brevent).setIcon(R.drawable.ic_block_black_24dp);
            brevent.getIcon().setTint(mColorControlNormal);
            brevent.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            menu.add(Menu.NONE, R.string.action_select_important, Menu.NONE,
                    R.string.action_select_important);
            menu.add(Menu.NONE, R.string.action_select_inverse, Menu.NONE,
                    R.string.action_select_inverse);
        }
        return super.onCreateOptionsMenu(menu);
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
            case R.string.action_restore:
                updateBrevent(false);
                break;
            case R.string.action_brevent:
                updateBrevent(true);
                break;
            case R.string.action_select_important:
                selectImportant();
                break;
            case R.string.action_select_inverse:
                selectInverse();
                break;
            case R.string.menu_feedback:
                if (BuildConfig.RELEASE) {
                    openFeedback();
                }
                break;
            case R.string.menu_guide:
                openGuide("menu");
                break;
            case R.string.menu_logs:
                fetchLogs();
                break;
            case R.string.menu_settings:
                openSettings();
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
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Guide")
                    .putContentType(type)
                    .putContentId("guide-" + type)
                    .putCustomAttribute("installer", installer));
            UILog.i("logContentView");
        }
    }

    private void openFeedback() {
        new AppsFeedbackFragment().show(getFragmentManager(), FRAGMENT_FEEDBACK);
    }

    @Override
    @CallSuper
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS) {
            if (mAdapter != null && updateAdapter(mAdapter)) {
                mAdapter.refreshFragment();
            }
            if (resultCode == Activity.RESULT_OK &&
                    data.getBooleanExtra(Intent.ACTION_CONFIGURATION_CHANGED, false)) {
                updateConfiguration();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
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

    private void updateConfiguration() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        BreventApplication application = (BreventApplication) getApplication();
        if (!"forcestop_only".equals(preferences.getString(BreventConfiguration.BREVENT_METHOD, ""))
                && !application.supportStandby()) {
            preferences.edit()
                    .putString(BreventConfiguration.BREVENT_METHOD, "forcestop_only").apply();
        }
        BreventConfiguration configuration = new BreventConfiguration(preferences);
        mHandler.obtainMessage(MESSAGE_BREVENT_REQUEST, configuration).sendToTarget();

        ComponentName componentName = new ComponentName(this, BreventReceiver.class);
        PackageManager packageManager = getPackageManager();
        int componentEnabled = packageManager.getComponentEnabledSetting(componentName);
        boolean allowReceiver = preferences.getBoolean(SettingsFragment.BREVENT_ALLOW_RECEIVER,
                SettingsFragment.DEFAULT_BREVENT_ALLOW_RECEIVER);
        if (configuration.allowRoot && allowReceiver) {
            if (componentEnabled != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                packageManager.setComponentEnabledSetting(componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }
        } else {
            if (componentEnabled == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                packageManager.setComponentEnabledSetting(componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }
    }

    private void openSettings() {
        Intent intent = new Intent(this, BreventSettings.class);
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

    private boolean onClickHome() {
        clearSelected();
        return true;
    }

    private void clearSelected() {
        AppsFragment fragment = getFragment();
        fragment.clearSelected();
        setSelectCount(0);
    }

    public void setSelectCount(int count) {
        boolean selectMode = count > 0;
        if (mSelectMode != selectMode) {
            invalidateOptionsMenu();
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(selectMode);
            }
        }
        mSelectMode = selectMode;
        if (mSelectMode) {
            if (mSnackBar != null && mSnackBar.isShown()) {
                mSnackBar.dismiss();
                mSnackBar = null;
            }
            mToolbar.setTitle(String.valueOf(count));
        } else {
            mToolbar.setTitle(R.string.app_name);
        }
    }

    public void onBreventResponse(@Nullable BreventProtocol response) {
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("response: " + response);
        }
        if (response == null) {
            uiHandler.sendEmptyMessage(UI_MESSAGE_NO_PERMISSION);
        } else {
            dispatchResponse(response);
        }
    }

    private void dispatchResponse(@NonNull BreventProtocol response) {
        int action = response.getAction();
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
            case BreventProtocol.STATUS_DISABLE_ROOT:
                showUnsupported(R.string.unsupported_disable_root);
                mHandler.removeCallbacksAndMessages(null);
                uiHandler.removeCallbacksAndMessages(null);
                break;
            default:
                break;
        }
    }

    private void onBreventNoEvent(BreventNoEvent response) {
        if (response.versionMismatched()) {
            showUnsupported(R.string.unsupported_version_mismatched);
            mHandler.removeCallbacksAndMessages(null);
            uiHandler.removeCallbacksAndMessages(null);
        } else if (response.mExit) {
            showUnsupported(R.string.unsupported_no_event);
            mHandler.removeCallbacksAndMessages(null);
            uiHandler.removeCallbacksAndMessages(null);
        } else {
            uiHandler.sendEmptyMessage(UI_MESSAGE_NO_EVENT);
            mHandler.sendEmptyMessageDelayed(MESSAGE_RETRIEVE2, DELAY);
            BreventApplication application = (BreventApplication) getApplication();
            if (!application.isEventMade()) {
                uiHandler.sendEmptyMessageDelayed(UI_MESSAGE_MAKE_EVENT, DELAY5);
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
                snackbar = Snackbar.make(mCoordinator, message, Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.action_message_undo, callback);
            } else {
                String message = getString(R.string.action_message_undone);
                snackbar = Snackbar.make(mCoordinator, message, Snackbar.LENGTH_LONG);
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
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("favorite: " + mFavorite);
        }
        if (hasGms()) {
            mFavorite.put(GMS, IMPORTANT_GMS);
            if (((BreventApplication) getApplication()).supportStopped()) {
                resolveGcmPackages(mGcm);
            }
        }

        int installedCount = getPackageManager().getInstalledPackages(0).size();
        if (mAdapter == null) {
            mAdapter = new AppsPagerAdapter(getFragmentManager(), mTitles);
            mInstalledCount = installedCount;
        } else if (mInstalledCount != installedCount) {
            mInstalledCount = installedCount;
            mAdapter.setExpired();
        }
        if (uiHandler == null) {
            return;
        }
        uiHandler.sendEmptyMessage(UI_MESSAGE_HIDE_PROGRESS);
        uiHandler.sendEmptyMessage(UI_MESSAGE_SHOW_PAGER);

        if (!hasResponse) {
            updateConfiguration();
            unregisterReceiver();
            hasResponse = true;
        }

        if (!mSelectMode && mBrevent.isEmpty()) {
            uiHandler.sendEmptyMessage(UI_MESSAGE_SHOW_SUCCESS);
        }
        unbreventImportant();
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
        makeDialerAndSms();
    }

    private void makeDialerAndSms() {
        if (mSms != null) {
            updatePriority(mSms);
        }
        if (mDialer != null) {
            updatePriority(mDialer);
        }
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
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showAllApps = sp.getBoolean(SettingsFragment.SHOW_ALL_APPS,
                SettingsFragment.DEFAULT_SHOW_ALL_APPS);
        boolean showFramework = sp.getBoolean(SettingsFragment.SHOW_FRAMEWORK_APPS,
                SettingsFragment.DEFAULT_SHOW_FRAMEWORK_APPS);
        return adapter.setShowAllApps(showAllApps) | adapter.setShowFramework(showFramework);
    }

    public void showFragmentAsync(AppsFragment fragment, long delayMillis) {
        Message message = uiHandler.obtainMessage(UI_MESSAGE_SHOW_FRAGMENT, fragment);
        uiHandler.sendMessageDelayed(message, delayMillis);
    }

    public void updateBreventResponse(BreventPackages breventPackages) {
        if (breventPackages.brevent) {
            mBrevent.addAll(breventPackages.packageNames);
            makeDialerAndSms();
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
        Snackbar.make(mCoordinator, message, Snackbar.LENGTH_LONG).show();
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
        showProgress(R.string.process_retrieving);
        BreventIntentService.startBrevent(this, BreventIntent.ACTION_RUN_AS_ROOT);
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
                Snackbar.LENGTH_INDEFINITE);
        mSnackBar.show();
    }

    public void unbrevent(String packageName) {
        UILog.i("will unbrevent: " + packageName);
        BreventPackages breventPackages = new BreventPackages(false, Collections.singleton
                (packageName));
        breventPackages.undoable = false;
        mHandler.obtainMessage(MESSAGE_BREVENT_REQUEST, breventPackages).sendToTarget();
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

    public boolean isStopped() {
        return stopped;
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
        return context.getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME +
                "(Android " + Locale.getDefault().toString() + "-" + Build.VERSION.RELEASE + ")";
    }

    public static void sendEmail(Context context, File path, String content) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("message/rfc822");
        if (path != null) {
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context,
                    BuildConfig.APPLICATION_ID + ".fileprovider", path));
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, getSubject(context));
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {BuildConfig.EMAIL});
        sendEmail(context, intent);
    }

    private static void sendEmail(Context context, Intent intent) {
        Intent email = getEmailIntent();
        PackageManager packageManager = context.getPackageManager();
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
                intents.add(new Intent(intent).setComponent(componentName));
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
