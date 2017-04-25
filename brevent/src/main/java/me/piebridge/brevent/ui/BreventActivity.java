package me.piebridge.brevent.ui;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArraySet;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.view.ViewPager;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toolbar;

import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import dalvik.system.PathClassLoader;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.override.HideApiOverride;
import me.piebridge.brevent.override.HideApiOverrideM;
import me.piebridge.brevent.override.HideApiOverrideN;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventPackages;
import me.piebridge.brevent.protocol.BreventPriority;
import me.piebridge.brevent.protocol.BreventProtocol;
import me.piebridge.brevent.protocol.BreventStatus;

public class BreventActivity extends Activity
        implements ViewPager.OnPageChangeListener, AppBarLayout.OnOffsetChangedListener {

    private static final String CUSTOM_TILE_PREFIX = "custom(";
    private static final int CUSTOM_TILE_LENGTH = 7;

    public static final int MESSAGE_RETRIEVE = 0;
    public static final int MESSAGE_RETRIEVE2 = 1;
    public static final int MESSAGE_BREVENT_RESPONSE = 2;
    public static final int MESSAGE_BREVENT_NO_RESPONSE = 3;
    public static final int MESSAGE_BREVENT_REQUEST = 4;
    public static final int MESSAGE_RETRIEVE3 = 5;

    public static final int UI_MESSAGE_SHOW_PROGRESS = 0;
    public static final int UI_MESSAGE_HIDE_PROGRESS = 1;
    public static final int UI_MESSAGE_SHOW_PAGER = 2;
    public static final int UI_MESSAGE_SHOW_FRAGMENT = 3;
    public static final int UI_MESSAGE_NO_BREVENT = 4;
    public static final int UI_MESSAGE_IO_BREVENT = 5;
    public static final int UI_MESSAGE_NO_BREVENT_DATA = 6;
    public static final int UI_MESSAGE_VERSION_UNMATCHED = 7;
    public static final int UI_MESSAGE_UPDATE_BREVENT = 8;
    public static final int UI_MESSAGE_HIDE_DISABLED = 9;
    public static final int UI_MESSAGE_UPDATE_PRIORITY = 10;
    public static final int UI_MESSAGE_SHOW_SUCCESS = 11;

    public static final int IMPORTANT_INPUT = 0;
    public static final int IMPORTANT_ALARM = 1;
    public static final int IMPORTANT_SMS = 2;
    public static final int IMPORTANT_HOME = 3;
    public static final int IMPORTANT_PERSISTENT = 4;
    public static final int IMPORTANT_ANDROID = 5;
    public static final int IMPORTANT_DIALER = 6;
    public static final int IMPORTANT_ASSISTANT = 7;
    public static final int IMPORTANT_WEBVIEW = 8;
    public static final int IMPORTANT_TILE = 9;
    public static final int IMPORTANT_ACCESSIBILITY = 10;
    public static final int IMPORTANT_DEVICE_ADMIN = 11;
    public static final int IMPORTANT_BATTERY = 13;
    public static final int IMPORTANT_TRUST_AGENT = 14;

    private static final int ROOT_TIMEOUT = 10000;

    private static final String FRAGMENT_DISABLED = "disabled";

    private static final String FRAGMENT_PROGRESS = "progress";

    private static final String FRAGMENT_PROGRESS_APPS = "progress_apps";

    private static final String FRAGMENT_FEEDBACK = "feedback";

    private static final String FRAGMENT_UNSUPPORTED = "unsupported";

    private static final int REQUEST_CODE_SETTINGS = 1;

    private ViewPager mPager;

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
    private AppBarLayout mAppBar;

    private Handler mHandler;
    private Handler uiHandler;

    private AppsReceiver mReceiver;

    private boolean mAppBarHidden = false;

    @ColorInt
    int mColorControlNormal;
    @ColorInt
    int mTextColorPrimary;
    @ColorInt
    int mColorControlHighlight;

    private String mLauncher;

    private volatile boolean stopped;

    private volatile boolean hasResponse;

    private Snackbar mSnackBar;

    private int mInstalledCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean disabledXposed = !BuildConfig.RELEASE;
        if (BuildConfig.SERVER != null) {
            String clazzServer = String.valueOf(BuildConfig.SERVER);
            try {
                ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
                PathClassLoader classLoader = new PathClassLoader(ai.sourceDir, systemClassLoader);
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
        if (BuildConfig.RELEASE) {
            verifySignatures();
        }
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!disabledXposed) {
            new UnsupportedFragment()
                    .setMessage(R.string.unsupported_xposed)
                    .show(getFragmentManager(), FRAGMENT_UNSUPPORTED);
        } else if (!BreventApplication.IS_OWNER) {
            new UnsupportedFragment().show(getFragmentManager(), FRAGMENT_UNSUPPORTED);
        } else if (preferences.getBoolean(BreventGuide.GUIDE, true)) {
            openGuide();
            finish();
        } else {
            setContentView(R.layout.activity_brevent);

            mCoordinator = (CoordinatorLayout) findViewById(R.id.coordinator);

            mToolbar = (Toolbar) findViewById(R.id.toolbar);
            setActionBar(mToolbar);

            mPager = (ViewPager) findViewById(R.id.pager);
            mPager.addOnPageChangeListener(this);

            ((TabLayout) findViewById(R.id.tabs)).setupWithViewPager(mPager);

            mAppBar = (AppBarLayout) findViewById(R.id.appbar);
            mAppBar.addOnOffsetChangedListener(this);

            uiHandler = new AppsActivityUIHandler(this);
            mHandler = new AppsActivityHandler(this, uiHandler);
            mReceiver = new AppsReceiver(mHandler, getToken().toString());

            mTitles = getResources().getStringArray(R.array.fragment_apps);

            mColorControlNormal = resolveColor(android.R.attr.colorControlNormal);
            mTextColorPrimary = resolveColor(android.R.attr.textColorPrimary);
            mColorControlHighlight = resolveColor(android.R.attr.colorControlHighlight);
        }
    }

    private void verifySignatures() {
        Signature[] signatures = getSignatures(getPackageManager(), BuildConfig.APPLICATION_ID);
        boolean equals = false;
        if (signatures != null) {
            for (Signature signature : signatures) {
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    byte[] sha1 = md.digest();
                    if (Arrays.equals(BuildConfig.SIGNATURE, sha1)) {
                        equals = true;
                        break;
                    }
                } catch (NoSuchAlgorithmException e) {
                    UILog.w("NoSuchAlgorithmException");
                }
            }
        }
        if (!equals) {
            UILog.e("invalid signature");
            finish();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mReceiver != null && hasResponse) {
            mHandler.sendEmptyMessage(MESSAGE_RETRIEVE2);
        }
    }

    public void showDisabled() {
        if (mHandler != null) {
            hideProgress();
            showDisabled(R.string.brevent_service_start);
        }
    }

    public void showDisabled(int title) {
        if (stopped) {
            return;
        }
        AppsDisabledFragment disabledFragment =
                (AppsDisabledFragment) getFragmentManager().findFragmentByTag(FRAGMENT_DISABLED);
        if (disabledFragment != null) {
            disabledFragment.dismiss();
        }
        disabledFragment = new AppsDisabledFragment();
        disabledFragment.update(title);
        disabledFragment.show(getFragmentManager(), FRAGMENT_DISABLED);
    }

    public void hideDisabled() {
        hideFragment(FRAGMENT_DISABLED);
    }

    public void showProgress(int message) {
        if (stopped) {
            return;
        }
        hideDisabled();
        ProgressFragment progressFragment =
                (ProgressFragment) getFragmentManager().findFragmentByTag(FRAGMENT_PROGRESS);
        if (progressFragment != null) {
            progressFragment.dismiss();
        }
        progressFragment = new ProgressFragment();
        progressFragment.updateMessage(message);
        progressFragment.show(getFragmentManager(), FRAGMENT_PROGRESS);
    }

    public void showAppProgress(int progress, int max, int size) {
        if (stopped) {
            return;
        }
        AppsProgressFragment progressFragment =
                (AppsProgressFragment) getFragmentManager().findFragmentByTag(FRAGMENT_PROGRESS_APPS);
        if (progressFragment == null) {
            progressFragment = new AppsProgressFragment();
            progressFragment.setTitle(R.string.process_retrieving_apps);
            progressFragment.update(progress, max, size);
            progressFragment.show(getFragmentManager(), FRAGMENT_PROGRESS_APPS);
        } else {
            progressFragment.update(progress, max, size);
        }
    }

    public void hideProgress() {
        hideFragment(FRAGMENT_PROGRESS);
    }

    public void hideAppProgress() {
        hideFragment(FRAGMENT_PROGRESS);
        hideFragment(FRAGMENT_PROGRESS_APPS);
    }

    private void hideFragment(String tag) {
        if (stopped) {
            return;
        }
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        stopped = false;
        if (mReceiver != null) {
            registerReceiver(mReceiver, new IntentFilter(BreventIntent.ACTION_BREVENT),
                    BreventIntent.PERMISSION_SHELL, mHandler);
            if (!hasResponse) {
                mHandler.sendEmptyMessage(MESSAGE_RETRIEVE);
            }
        }
    }

    @Override
    protected void onStop() {
        stopped = true;
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mHandler.removeCallbacksAndMessages(null);
            uiHandler.removeCallbacksAndMessages(null);
        }
        super.onStop();
    }

    private void dismissDialogFragmentIfNeeded(boolean allowStateLoss) {
        dismissDialogFragmentIfNeeded(FRAGMENT_DISABLED, allowStateLoss);
        dismissDialogFragmentIfNeeded(FRAGMENT_PROGRESS, allowStateLoss);
        dismissDialogFragmentIfNeeded(FRAGMENT_UNSUPPORTED, allowStateLoss);
    }

    private void dismissDialogFragmentIfNeeded(String tag, boolean allowStateLoss) {
        Fragment fragment = getFragmentManager().findFragmentByTag(tag);
        if (fragment instanceof DialogFragment) {
            DialogFragment dialogFragment = (DialogFragment) fragment;
            if (dialogFragment.isVisible()) {
                if (allowStateLoss) {
                    dialogFragment.dismissAllowingStateLoss();
                } else {
                    dialogFragment.dismiss();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mReceiver != null) {
            if (mHandler != null && mHandler.getLooper() != null) {
                mHandler.getLooper().quit();
            }
            mHandler = null;
            uiHandler = null;
        }
        dismissDialogFragmentIfNeeded(true);
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
            mAppBar.setExpanded(true);
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
        } else if (BreventStatus.isStandby(status)) {
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
            if (BreventStatus.isProcess(processState)) {
                total++;
                if (BreventStatus.isTop(processState)) {
                    top++;
                } else if (BreventStatus.isService(processState)) {
                    service++;
                } else if (BreventStatus.isCached(processState)) {
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
        if (BreventStatus.isStandby(status)) {
            return R.drawable.ic_snooze_black_24dp;
        } else if (BreventStatus.isRunning(status)) {
            return R.drawable.ic_alarm_black_24dp;
        } else {
            return R.drawable.ic_block_black_24dp;
        }
    }

    public boolean isPriority(String packageName) {
        return mPriority.contains(packageName);
    }

    public int getInactive(String packageName) {
        return BreventStatus.getInactive(mProcesses.get(packageName));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        if (!mSelectMode) {
            if (BuildConfig.RELEASE) {
                menu.add(Menu.NONE, R.string.menu_feedback, Menu.NONE, R.string.menu_feedback);
            }
            menu.add(Menu.NONE, R.string.menu_guide, Menu.NONE, R.string.menu_guide);
            menu.add(Menu.NONE, R.string.menu_settings, Menu.NONE, R.string.menu_settings);
        } else {
            MenuItem remove =
                    menu.add(Menu.NONE, R.string.action_restore, Menu.NONE, R.string.action_restore)
                            .setIcon(R.drawable.ic_panorama_fish_eye_black_24dp);
            remove.getIcon().setTint(mColorControlNormal);
            remove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            MenuItem brevent =
                    menu.add(Menu.NONE, R.string.action_brevent, Menu.NONE, R.string.action_brevent)
                            .setIcon(R.drawable.ic_block_black_24dp);
            brevent.getIcon().setTint(mColorControlNormal);
            brevent.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            menu.add(Menu.NONE, R.string.action_select_important, Menu.NONE,
                    R.string.action_select_important);
            menu.add(Menu.NONE, R.string.action_select_inverse, Menu.NONE,
                    R.string.action_select_inverse);
        }
        return super.onCreateOptionsMenu(menu);
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
                openGuide();
                break;
            case R.string.menu_settings:
                openSettings();
                break;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    public void openGuide() {
        startActivity(new Intent(this, BreventGuide.class));
    }

    private void openFeedback() {
        if (stopped) {
            return;
        }
        new AppsFeedbackFragment().show(getFragmentManager(), FRAGMENT_FEEDBACK);
    }

    @Override
    @CallSuper
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS) {
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
            if (fragment.getSelectedSize() > 0) {
                fragment.clearSelected();
                return;
            }
        }
        super.onBackPressed();
    }

    private void updateConfiguration() {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        BreventApplication application = (BreventApplication) getApplication();
        if (!application.supportStandby() && !"forcestop_only".equals(
                preferences.getString(BreventConfiguration.BREVENT_METHOD, ""))) {
            preferences.edit().putString(BreventConfiguration.BREVENT_METHOD,
                    "forcestop_only").apply();
        }
        BreventConfiguration configuration = new BreventConfiguration(getToken(), preferences);
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
            BreventPackages breventPackages = new BreventPackages(brevent, getToken(), selected);
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

    @Override
    public void onOffsetChanged(AppBarLayout appBar, int offset) {
        boolean appBarHidden = appBar.getHeight() + offset == 0;
        if (appBarHidden != mAppBarHidden) {
            mAppBarHidden = appBarHidden;
            getWindow().getDecorView().setSystemUiVisibility(
                    mAppBarHidden ? View.SYSTEM_UI_FLAG_LOW_PROFILE : 0);
        }
    }

    public void onBreventResponse(BreventProtocol response) {
        if (stopped) {
            return;
        }
        int action = response.getAction();
        switch (action) {
            case BreventProtocol.STATUS_RESPONSE:
                if (!hasResponse) {
                    hasResponse = true;
                }
                onBreventStatusResponse((BreventStatus) response);
                break;
            case BreventProtocol.UPDATE_BREVENT:
                onBreventPackagesResponse((BreventPackages) response);
                break;
            case BreventProtocol.UPDATE_PRIORITY:
                onBreventPriorityResponse((BreventPriority) response);
                break;
            default:
                break;
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
                snackbar.setAction(R.string.action_snackbar_undo, callback);
            } else {
                snackbar = Snackbar.make(mCoordinator, R.string.action_snackbar_undone,
                        Snackbar.LENGTH_LONG);
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
        AppsFragment fragment = getFragment();
        fragment.update(breventPriority.packageNames);
    }

    private void onBreventPriorityResponse(BreventPriority response) {
        if (!response.packageNames.isEmpty()) {
            uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_UPDATE_PRIORITY,
                    response).sendToTarget();
        }
    }

    private void onBreventStatusResponse(BreventStatus status) {
        BreventApplication application = (BreventApplication) getApplication();
        application.updateStatus(status);

        mProcesses.clear();
        mProcesses.putAll(status.mProcesses);

        mBrevent.clear();
        mBrevent.addAll(status.mBrevent);

        mPriority.clear();
        mPriority.addAll(status.mPriority);

        mImportant.clear();
        mFavorite.clear();
        mGcm.clear();
        resolveImportantPackages(status.mProcesses, mImportant);
        for (String packageName : status.mTrustAgents) {
            mImportant.put(packageName, IMPORTANT_TRUST_AGENT);
        }
        for (String packageName : status.mAndroidProcesses) {
            mImportant.put(packageName, IMPORTANT_ANDROID);
        }
        resolveFavoritePackages(mFavorite);
        if (((BreventApplication) getApplication()).supportStopped()) {
            resolveGcmPackages(mGcm);
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

        if (!getToken().equals(status.getToken())) {
            updateConfiguration();
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
            BreventPackages breventPackages =
                    new BreventPackages(false, getToken(), importantBrevented);
            breventPackages.undoable = false;
            mHandler.obtainMessage(MESSAGE_BREVENT_REQUEST, breventPackages).sendToTarget();
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

    private void resolveImportantPackages(SimpleArrayMap<String, SparseIntArray> processes,
                                          SimpleArrayMap<String, Integer> packageNames) {
        // input method
        String input = getPackageName(getSecureSetting(Settings.Secure.DEFAULT_INPUT_METHOD));
        if (input != null) {
            packageNames.put(input, IMPORTANT_INPUT);
        }

        // next alarm
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo alarmClock = alarmManager.getNextAlarmClock();
        if (alarmClock != null && alarmClock.getShowIntent() != null) {
            String alarmClockPackage = alarmClock.getShowIntent().getCreatorPackage();
            packageNames.put(alarmClockPackage, IMPORTANT_ALARM);
        }

        // sms
        packageNames.put(getSecureSetting(HideApiOverride.SMS_DEFAULT_APPLICATION), IMPORTANT_SMS);

        // dialer
        String dialer = getDefaultApp(Intent.ACTION_DIAL);
        if (dialer != null) {
            packageNames.put(dialer, IMPORTANT_DIALER);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // dialer
            dialer = ((TelecomManager) getSystemService(TELECOM_SERVICE))
                    .getDefaultDialerPackage();
            if (dialer != null) {
                packageNames.put(dialer, IMPORTANT_DIALER);
            }
        }

        // assistant
        String assistant;
        assistant = getPackageName(getSecureSetting(HideApiOverride.getVoiceInteractionService()));
        if (assistant != null) {
            packageNames.put(assistant, IMPORTANT_ASSISTANT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assistant = getPackageName(getSecureSetting(HideApiOverride.getAssistant()));
            if (assistant != null) {
                packageNames.put(assistant, IMPORTANT_ASSISTANT);
            }
        }

        // webview
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String webView = Settings.Global.getString(getContentResolver(),
                    HideApiOverrideN.WEBVIEW_PROVIDER);
            packageNames.put(webView, IMPORTANT_WEBVIEW);
        }

        // launcher
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo =
                getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null) {
            mLauncher = resolveInfo.activityInfo.packageName;
            packageNames.put(mLauncher, IMPORTANT_HOME);
        }

        AccessibilityManager accessibilityManager =
                (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledAccessibilityServiceList =
                accessibilityManager.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo accessibilityServiceInfo : enabledAccessibilityServiceList) {
            packageNames.put(accessibilityServiceInfo.getResolveInfo().serviceInfo.packageName,
                    IMPORTANT_ACCESSIBILITY);
        }

        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        List<ComponentName> componentNames = devicePolicyManager.getActiveAdmins();
        if (componentNames != null) {
            for (ComponentName componentName : componentNames) {
                packageNames.put(componentName.getPackageName(), IMPORTANT_DEVICE_ADMIN);
            }
        }

        // persistent
        int size = processes.size();
        for (int i = 0; i < size; ++i) {
            if (BreventStatus.isPersistent(processes.valueAt(i))) {
                packageNames.put(processes.keyAt(i), IMPORTANT_PERSISTENT);
            }
        }

        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("important: " + packageNames);
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
        List<PackageInfo> packageInfos =
                getPackageManager().getPackagesHoldingPermissions(new String[] {
                        "com.google.android.c2dm.permission.RECEIVE",
                }, 0);
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

    private void resolveFavoritePackages(SimpleArrayMap<String, Integer> packageNames) {
        // battery
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                IDeviceIdleController deviceidle = IDeviceIdleController.Stub.asInterface(
                        ServiceManager.getService("deviceidle"));
                String[] fullPowerWhitelist = deviceidle.getFullPowerWhitelist();
                if (fullPowerWhitelist != null) {
                    for (String s : fullPowerWhitelist) {
                        packageNames.put(s, IMPORTANT_BATTERY);
                    }
                }
            } catch (RemoteException e) {
                // do nothing
            }
        }

        // tile
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String tiles = getSecureSetting(HideApiOverrideN.QS_TILES);
            for (String packageName : parseTiles(tiles)) {
                packageNames.put(packageName, IMPORTANT_TILE);
            }
        }

        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("favorite: " + packageNames);
        }
    }

    public void showViewPager() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showAllApps = sp.getBoolean(SettingsFragment.SHOW_ALL_APPS,
                SettingsFragment.DEFAULT_SHOW_ALL_APPS);
        boolean showFramework = sp.getBoolean(SettingsFragment.SHOW_FRAMEWORK_APPS,
                SettingsFragment.DEFAULT_SHOW_FRAMEWORK_APPS);
        mAdapter.setShowAllApps(showAllApps);
        mAdapter.setShowFramework(showFramework);
        if (mPager.getAdapter() == null) {
            mPager.setAdapter(mAdapter);
        } else {
            mAdapter.refreshFragment();
        }
    }

    public void showFragmentAsync(AppsFragment fragment, long delayMillis) {
        if (stopped) {
            return;
        }
        Message message = uiHandler.obtainMessage(UI_MESSAGE_SHOW_FRAGMENT, fragment);
        uiHandler.sendMessageDelayed(message, delayMillis);
    }

    public UUID getToken() {
        return ((BreventApplication) getApplication()).getToken();
    }

    public void updateBreventResponse(BreventPackages breventPackages) {
        if (breventPackages.brevent) {
            mBrevent.addAll(breventPackages.packageNames);
        } else {
            mBrevent.removeAll(breventPackages.packageNames);
        }
        AppsFragment fragment = getFragment();
        if (!breventPackages.undoable) {
            fragment.select(breventPackages.packageNames);
        } else {
            fragment.update(breventPackages.packageNames);
        }
    }

    public void copy(String content) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, content));
    }

    public void showSnackbar(String message) {
        Snackbar.make(mCoordinator, message, Snackbar.LENGTH_LONG).show();
    }

    public int resolveColor(int resId) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(resId, tv, true);
        if (tv.type == TypedValue.TYPE_NULL) {
            return Color.BLACK;
        } else if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return tv.data;
        } else {
            return ContextCompat.getColor(this, tv.resourceId);
        }
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
            case IMPORTANT_TILE:
                return getString(R.string.important_tile, label);
            case IMPORTANT_ACCESSIBILITY:
                return getString(R.string.important_accessibility, label);
            case IMPORTANT_DEVICE_ADMIN:
                return getString(R.string.important_device_admin, label);
            case IMPORTANT_BATTERY:
                return getString(R.string.important_battery, label);
            case IMPORTANT_TRUST_AGENT:
                return getString(R.string.important_trust_agent, label);
            default:
                break;
        }
        return label;
    }

    public void runAsRoot(String path) {
        showProgress(R.string.process_retrieving);
        BreventIntentService.startBrevent(this, BreventIntent.ACTION_BREVENT);
        mHandler.sendEmptyMessageDelayed(MESSAGE_RETRIEVE2, ROOT_TIMEOUT);
    }

    public void updatePriority(String packageName, boolean priority) {
        BreventPriority breventPriority =
                new BreventPriority(priority, getToken(), Collections.singleton(packageName));
        mHandler.obtainMessage(MESSAGE_BREVENT_REQUEST, breventPriority).sendToTarget();
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
        BreventPackages breventPackages =
                new BreventPackages(false, getToken(), Collections.singleton(packageName));
        breventPackages.undoable = false;
        mHandler.obtainMessage(MESSAGE_BREVENT_REQUEST, breventPackages).sendToTarget();
    }

    public static Signature[] getSignatures(PackageManager packageManager, String packageName) {
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            String codePath = applicationInfo.sourceDir;
            PackageParser.Package pkg = new PackageParser.Package(codePath);
            pkg.baseCodePath = codePath;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageParser.collectCertificates(pkg, PackageManager.GET_SIGNATURES);
            } else {
                HideApiOverrideM.collectCertificates(pkg, PackageManager.GET_SIGNATURES);
            }
            return pkg.mSignatures;
        } catch (PackageManager.NameNotFoundException | PackageParser.PackageParserException e) {
            // do nothing
            return null;
        }
    }

    public static Collection<String> parseTiles(String tiles) {
        if (TextUtils.isEmpty(tiles)) {
            return Collections.emptyList();
        }
        Collection<String> packageNames = new ArrayList<>();
        for (String tile : tiles.split(",")) {
            // custom(com.github.shadowsocks/.ShadowsocksTileService)
            if (tile.startsWith(CUSTOM_TILE_PREFIX) && tile.endsWith(")")) {
                int index = tile.indexOf('/');
                if (index > 0) {
                    packageNames.add(tile.substring(CUSTOM_TILE_LENGTH, index));
                }
            }
        }
        return packageNames;
    }

}
