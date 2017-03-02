package me.piebridge.brevent.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.view.ViewPager;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toolbar;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventPackages;
import me.piebridge.brevent.protocol.BreventProtocol;
import me.piebridge.brevent.protocol.BreventStatus;

public class BreventActivity extends Activity implements ViewPager.OnPageChangeListener, AppBarLayout.OnOffsetChangedListener {

    public static final int MESSAGE_RETRIEVE = 0;
    public static final int MESSAGE_RETRIEVE2 = 1;
    public static final int MESSAGE_BREVENT_RESPONSE = 2;
    public static final int MESSAGE_BREVENT_NO_RESPONSE = 3;
    public static final int MESSAGE_BREVENT_REQUEST = 4;

    public static final int UI_MESSAGE_SHOW_PROGRESS = 0;
    public static final int UI_MESSAGE_HIDE_PROGRESS = 1;
    public static final int UI_MESSAGE_SHOW_PAGER = 2;
    public static final int UI_MESSAGE_SHOW_FRAGMENT = 3;
    public static final int UI_MESSAGE_NO_BREVENT = 4;
    public static final int UI_MESSAGE_IO_BREVENT = 5;
    public static final int UI_MESSAGE_NO_BREVENT_DATA = 6;
    public static final int UI_MESSAGE_VERSION_UNMATCHED = 7;
    public static final int UI_MESSAGE_UPDATE_BREVENT = 8;

    public static final int IMPORTANT_INPUT = 0;
    public static final int IMPORTANT_ALARM = 1;
    public static final int IMPORTANT_SMS = 2;
    public static final int IMPORTANT_LAUNCHER = 3;
    public static final int IMPORTANT_PERSISTENT = 4;
    public static final int IMPORTANT_ANDROID = 5;

    private static final String FRAGMENT_DISABLED = "disabled";

    private static final String FRAGMENT_PROGRESS = "progress";

    private static final String FRAGMENT_PROGRESS_APPS = "progress_apps";

    private static final String FRAGMENT_FEEDBACK = "feedback";

    private static final int REQUEST_CODE_SETTINGS = 1;

    private ViewPager mPager;

    private AppsPagerAdapter mAdapter;

    private String[] mTitles;

    private SimpleArrayMap<String, SparseIntArray> mProcesses = new SimpleArrayMap<>();
    private Set<String> mBrevent = new ArraySet<>();
    private SimpleArrayMap<String, Integer> mImportant = new SimpleArrayMap<>();

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

    private AppsDisabledFragment mDisabledFragment;

    private String mLauncher;

    private boolean stopped;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // disable hooks
            Field disableHooks = XposedBridge.class.getDeclaredField("disableHooks");
            disableHooks.setAccessible(true);
            disableHooks.set(null, true);

            // replace hooked method callbacks
            Field sHookedMethodCallbacks = XposedBridge.class.getDeclaredField("sHookedMethodCallbacks");
            sHookedMethodCallbacks.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Member, XposedBridge.CopyOnWriteSortedSet<XC_MethodHook>> map = (Map<Member, XposedBridge.CopyOnWriteSortedSet<XC_MethodHook>>) sHookedMethodCallbacks.get(null);
            for (XposedBridge.CopyOnWriteSortedSet<XC_MethodHook> hooked : map.values()) {
                Object[] snapshot = hooked.getSnapshot();
                int length = snapshot.length;
                for (int i = 0; i < length; ++i) {
                    snapshot[i] = XC_MethodReplacement.DO_NOTHING;
                }
            }
        } catch (Throwable t) { // NOSONAR
            // do nothing
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (preferences.getBoolean(BreventGuide.GUIDE, true)) {
            startActivity(new Intent(this, BreventGuide.class));
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

            mHandler.sendEmptyMessage(MESSAGE_RETRIEVE);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mHandler.sendEmptyMessage(MESSAGE_RETRIEVE2);
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
        if (mDisabledFragment == null) {
            mDisabledFragment = new AppsDisabledFragment();
            mDisabledFragment.update(title);
            mDisabledFragment.show(getFragmentManager(), FRAGMENT_DISABLED);
        } else {
            mDisabledFragment.update(title);
        }

    }

    public void hideDisabled() {
        hideFragment(FRAGMENT_DISABLED);
    }

    public void showProgress(int message) {
        if (stopped) {
            return;
        }
        hideDisabled();
        ProgressFragment progressFragment = (ProgressFragment) getFragmentManager().findFragmentByTag(FRAGMENT_PROGRESS);
        if (progressFragment == null) {
            progressFragment = new ProgressFragment();
            progressFragment.updateMessage(message);
            progressFragment.show(getFragmentManager(), FRAGMENT_PROGRESS);
        }
    }

    public void showAppProgress(int progress, int max, int size) {
        if (stopped) {
            return;
        }
        AppsProgressFragment progressFragment = (AppsProgressFragment) getFragmentManager().findFragmentByTag(FRAGMENT_PROGRESS_APPS);
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
        hideFragment(FRAGMENT_PROGRESS_APPS);
    }

    private void hideFragment(String tag) {
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        stopped = false;
        registerReceiver(mReceiver, new IntentFilter(BreventIntent.ACTION_BREVENT), BreventIntent.PERMISSION_SHELL, mHandler);
    }

    @Override
    protected void onStop() {
        stopped = true;
        dismissDialogFragmentIfNeeded(true);
        unregisterReceiver(mReceiver);
        mHandler.removeCallbacksAndMessages(null);
        uiHandler.removeCallbacksAndMessages(null);
        super.onStop();
    }

    private void dismissDialogFragmentIfNeeded(boolean allowStateLoss) {
        dismissDialogFragmentIfNeeded(FRAGMENT_DISABLED, allowStateLoss);
        dismissDialogFragmentIfNeeded(FRAGMENT_PROGRESS, allowStateLoss);
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
        if (mHandler != null && mHandler.getLooper() != null) {
            mHandler.getLooper().quit();
        }
        mHandler = null;
        uiHandler = null;
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
        return BreventStatus.formatDescription(this, mProcesses.get(packageName));
    }

    @DrawableRes
    public int getStatusIcon(String packageName) {
        if (!mBrevent.contains(packageName)) {
            return 0;
        }
        SparseIntArray status = mProcesses.get(packageName);
        if (BreventStatus.isStandby(status)) {
            return R.drawable.ic_snooze_black_24dp;
        } else if (BreventStatus.getInactive(status) > 0) {
            return R.drawable.ic_alarm_black_24dp;
        } else {
            return R.drawable.ic_block_black_24dp;
        }
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
            menu.add(Menu.NONE, R.string.menu_settings, Menu.NONE, R.string.menu_settings);
        } else {
            MenuItem remove = menu.add(Menu.NONE, R.string.action_restore, Menu.NONE, R.string.action_restore)
                    .setIcon(R.drawable.ic_panorama_fish_eye_black_24dp);
            remove.getIcon().setTint(mColorControlNormal);
            remove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            MenuItem brevent = menu.add(Menu.NONE, R.string.action_brevent, Menu.NONE, R.string.action_brevent)
                    .setIcon(R.drawable.ic_block_black_24dp);
            brevent.getIcon().setTint(mColorControlNormal);
            brevent.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            menu.add(Menu.NONE, R.string.action_select_important, Menu.NONE, R.string.action_select_important);
            menu.add(Menu.NONE, R.string.action_select_inverse, Menu.NONE, R.string.action_select_inverse);
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
            case R.string.menu_settings:
                openSettings();
                break;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
        return true;
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
            if (resultCode == Activity.RESULT_OK && data.getBooleanExtra(Intent.ACTION_CONFIGURATION_CHANGED, false)) {
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        BreventConfiguration configuration = new BreventConfiguration(getToken(), preferences);
        mHandler.obtainMessage(MESSAGE_BREVENT_REQUEST, configuration).sendToTarget();
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
        if (fragment.isAllImportant()) {
            fragment.selectAll();
        } else {
            fragment.select(getImportant());
        }
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

    public boolean isLauncher(String packageName) {
        return mLauncher != null && mLauncher.equals(packageName);
    }

    private boolean updateBrevent(boolean brevent) {
        AppsFragment fragment = getFragment();
        if (!fragment.isAllImportant()) {
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
            getWindow().getDecorView().setSystemUiVisibility(mAppBarHidden ? View.SYSTEM_UI_FLAG_LOW_PROFILE : 0);
        }
    }

    public void onBreventResponse(BreventProtocol response) {
        int action = response.getAction();
        switch (action) {
            case BreventProtocol.STATUS_RESPONSE:
                onBreventStatusResponse((BreventStatus) response);
                break;
            case BreventProtocol.UPDATE_BREVENT:
                onBreventPackagesResponse((BreventPackages) response);
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
                String message = getString(response.brevent ? R.string.action_brevent_results : R.string.action_restore_results, response.packageNames.size());
                snackbar = Snackbar.make(mCoordinator, message, Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.action_snackbar_undo, callback);
            } else {
                snackbar = Snackbar.make(mCoordinator, R.string.action_snackbar_undone, Snackbar.LENGTH_LONG);
            }
            snackbar.addCallback(callback);
            snackbar.show();
        }
    }

    private void onBreventStatusResponse(BreventStatus status) {
        mProcesses.clear();
        mProcesses.putAll(status.getProcesses());

        mBrevent.clear();
        mBrevent.addAll(status.getBrevent());

        mImportant.clear();
        resolveImportantPackages(status.getProcesses(), mImportant);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showFramework = sp.getBoolean(SettingsFragment.SHOW_FRAMEWORK_APPS, SettingsFragment.DEFAULT_SHOW_FRAMEWORK_APPS);
        boolean showAllApps = sp.getBoolean(SettingsFragment.SHOW_ALL_APPS, SettingsFragment.DEFAULT_SHOW_ALL_APPS);
        if (mAdapter == null) {
            mAdapter = new AppsPagerAdapter(getFragmentManager(), mTitles, showFramework);
        } else {
            mAdapter.setShowFramework(showFramework);
        }
        mAdapter.setShowAllApps(showAllApps);
        uiHandler.sendEmptyMessage(UI_MESSAGE_HIDE_PROGRESS);
        uiHandler.sendEmptyMessage(UI_MESSAGE_SHOW_PAGER);

        if (!getToken().equals(status.getToken())) {
            updateConfiguration();
        }
    }

    private void resolveImportantPackages(SimpleArrayMap<String, SparseIntArray> processes, SimpleArrayMap<String, Integer> packageNames) {
        // enabled input method
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> inputMethods = inputMethodManager.getEnabledInputMethodList();
        if (inputMethods != null) {
            for (InputMethodInfo inputMethod : inputMethods) {
                packageNames.put(inputMethod.getPackageName(), IMPORTANT_INPUT);
            }
        }

        // alarm
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo alarmClock = alarmManager.getNextAlarmClock();
        if (alarmClock != null && alarmClock.getShowIntent() != null) {
            String alarmClockPackage = alarmClock.getShowIntent().getCreatorPackage();
            packageNames.put(alarmClockPackage, IMPORTANT_ALARM);
        }

        // sms
        String sms = Settings.Secure.getString(getContentResolver(), "sms_default_application");
        packageNames.put(sms, IMPORTANT_SMS);

        // launcher
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null) {
            mLauncher = resolveInfo.activityInfo.packageName;
            packageNames.put(mLauncher, IMPORTANT_LAUNCHER);
        }

        int size = processes.size();
        for (int i = 0; i < size; ++i) {
            if (BreventStatus.isPersistent(processes.valueAt(i))) {
                packageNames.put(processes.keyAt(i), IMPORTANT_PERSISTENT);
            }
        }
    }

    public void showViewPager() {
        if (mPager.getAdapter() == null) {
            mPager.setAdapter(mAdapter);
        } else {
            mAdapter.refreshFragment();
        }
    }

    public void showFragmentAsync(AppsFragment fragment, long delayMillis) {
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
        } else if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return tv.data;
        } else {
            return ContextCompat.getColor(this, tv.resourceId);
        }
    }

    public String getImportantLabel(String label, String packageName) {
        int index = mImportant.indexOfKey(packageName);
        if (index >= 0) {
            switch (mImportant.valueAt(index)) {
                case IMPORTANT_INPUT:
                    return getString(R.string.important_input, label);
                case IMPORTANT_ALARM:
                    return getString(R.string.important_alarm, label);
                case IMPORTANT_SMS:
                    return getString(R.string.important_sms, label);
                case IMPORTANT_LAUNCHER:
                    return getString(R.string.important_launcher, label);
                case IMPORTANT_PERSISTENT:
                    return getString(R.string.important_persistent, label);
                case IMPORTANT_ANDROID:
                    return getString(R.string.important_android, label);
                default:
                    break;
            }
        }
        return label;
    }

}
