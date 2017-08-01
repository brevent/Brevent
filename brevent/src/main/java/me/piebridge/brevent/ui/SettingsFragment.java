package me.piebridge.brevent.ui;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.Log;

import java.text.DecimalFormat;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.donation.DonateActivity;

/**
 * Created by thom on 2017/2/8.
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    public static final String SHOW_DONATION = "show_donation";

    public static final String SHOW_ALL_APPS = "show_all_apps";
    public static final boolean DEFAULT_SHOW_ALL_APPS = false;

    public static final String SHOW_FRAMEWORK_APPS = "show_framework_apps";
    public static final boolean DEFAULT_SHOW_FRAMEWORK_APPS = false;

    public static final String IS_PLAY = "is_play";

    private static final String FRAGMENT_DONATE = "donate";

    private PreferenceCategory breventExperimental;

    private SwitchPreference preferenceOptimizeVpn;
    private SwitchPreference preferenceAbnormalBack;
    private SwitchPreference preferenceAllowRoot;
    private SwitchPreference preferenceDonation;

    private Preference preferenceStandbyTimeout;

    private int repeat = 0;
    private int mCount;

    public SettingsFragment() {
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        PreferenceScreen preferenceScreen = getPreferenceScreen();

        breventExperimental = (PreferenceCategory) preferenceScreen
                .findPreference("brevent_experimental");

        preferenceOptimizeVpn = (SwitchPreference) preferenceScreen
                .findPreference(BreventConfiguration.BREVENT_OPTIMIZE_VPN);
        preferenceAbnormalBack = (SwitchPreference) preferenceScreen
                .findPreference(BreventConfiguration.BREVENT_ABNORMAL_BACK);
        preferenceAllowRoot = (SwitchPreference) preferenceScreen
                .findPreference(BreventConfiguration.BREVENT_ALLOW_ROOT);

        preferenceDonation = (SwitchPreference) preferenceScreen.findPreference(SHOW_DONATION);

        preferenceStandbyTimeout = preferenceScreen
                .findPreference(BreventConfiguration.BREVENT_STANDBY_TIMEOUT);
        BreventApplication application = (BreventApplication) getActivity().getApplication();
        if (!application.supportStandby()) {
            ((PreferenceCategory) preferenceScreen.findPreference("brevent_list"))
                    .removePreference(preferenceStandbyTimeout);
        }

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (BuildConfig.RELEASE) {
            preferenceOptimizeVpn.setEnabled(false);
            preferenceAbnormalBack.setEnabled(false);
            preferenceAllowRoot.setEnabled(false);
        } else {
            ((PreferenceCategory) preferenceScreen.findPreference("brevent_about"))
                    .removePreference(preferenceDonation);
            preferenceOptimizeVpn.setSummary(R.string.brevent_optimize_vpn_label_debug);
            preferenceAbnormalBack.setSummary(R.string.brevent_abnormal_back_label_debug);
            preferenceAllowRoot.setSummary(R.string.brevent_allow_root_label_debug);
        }
        if (!"root".equals(application.getMode())
                && !sharedPreferences.getBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, false)) {
            breventExperimental.removePreference(preferenceAllowRoot);
            preferenceScreen.findPreference("brevent_about_version")
                    .setOnPreferenceClickListener(this);
        }
        if (BuildConfig.RELEASE) {
            double donation = application.getDonation();
            if (donation > 0) {
                preferenceDonation.setSummary(getString(R.string.show_donation_rmb,
                        new DecimalFormat("#.##").format(donation)));
                preferenceOptimizeVpn.setEnabled(true);
                preferenceAbnormalBack.setEnabled(true);
            }
            if (donation >= BreventSettings.donateAmount() * 5) {
                preferenceOptimizeVpn.setEnabled(true);
                preferenceAbnormalBack.setEnabled(true);
                preferenceAllowRoot.setEnabled(true);
            } else if (!application.hasPlay()) {
                preferenceAllowRoot.setEnabled(false);
                preferenceAllowRoot.setChecked(false);
                if ("root".equals(application.getMode())) {
                    showDonate(true);
                }
            }
        }
        onUpdateBreventMethod();
    }

    public void showDonate(boolean root) {
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("show " + FRAGMENT_DONATE);
        }
        AppsDonateFragment fragment = (AppsDonateFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_DONATE);
        if (fragment != null) {
            fragment.dismiss();
        }
        fragment = new AppsDonateFragment();
        fragment.setRoot(root);
        fragment.show(getFragmentManager(), FRAGMENT_DONATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        onShowDonationChanged();
        boolean adbRunning = SystemProperties.get("init.svc.adbd", Build.UNKNOWN).equals("running");
        Preference preference = getPreferenceScreen().findPreference("brevent_about_developer");
        if (adbRunning) {
            preference.setSummary(R.string.brevent_about_developer_adb);
        } else {
            preference.setSummary(null);
        }
        preference.setOnPreferenceClickListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SHOW_DONATION.equals(key)) {
            onShowDonationChanged();
        } else if (BreventConfiguration.BREVENT_METHOD.equals(key)) {
            onUpdateBreventMethod();
        }
    }

    private void onUpdateBreventMethod() {
        String method = getPreferenceScreen().getSharedPreferences()
                .getString(BreventConfiguration.BREVENT_METHOD, null);
        if ("standby_forcestop".equals(method)) {
            preferenceStandbyTimeout.setEnabled(true);
        } else {
            preferenceStandbyTimeout.setEnabled(false);
        }
    }

    private void onShowDonationChanged() {
        boolean showDonation = BuildConfig.RELEASE &&
                getPreferenceScreen().getSharedPreferences().getBoolean(SHOW_DONATION, true);
        ((DonateActivity) getActivity()).showDonation(showDonation);
    }

    public void updatePlayDonation(int total, boolean contributor) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        BreventApplication application = (BreventApplication) activity.getApplication();
        String summary;
        double donation = application.getDonation();
        String rmb = donation > 0 ? new DecimalFormat("#.##").format(donation) : "";
        boolean hasAlipay = !TextUtils.isEmpty(rmb);
        if (contributor) {
            if (total > 0) {
                if (hasAlipay) {
                    summary = getString(R.string.show_donation_play_and_rmb_and_contributor,
                            total, rmb);
                } else {
                    summary = getString(R.string.show_donation_play_and_contributor, total);
                }
            } else {
                if (hasAlipay) {
                    summary = getString(R.string.show_donation_rmb_and_contributor, rmb);
                } else {
                    summary = getString(R.string.show_donation_contributor);
                }
            }

        } else {
            if (total > 0) {
                if (hasAlipay) {
                    summary = getString(R.string.show_donation_play_and_rmb,
                            total, rmb);
                } else {
                    summary = getString(R.string.show_donation_play, total);
                }
            } else {
                if (hasAlipay) {
                    summary = getString(R.string.show_donation_rmb, rmb);
                } else {
                    summary = null;
                }
            }
        }
        if (summary != null) {
            preferenceDonation.setSummary(summary);
        }
        int count = total + (contributor ? BreventSettings.CONTRIBUTOR : 0);
        if (donation > 0) {
            count += (int) (donation / 5);
        }
        mCount = count;
        if (getArguments().getBoolean(IS_PLAY, false)) {
            updatePlayVersion(count);
        } else if (count < BreventSettings.donateAmount()) {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceAllowRoot.setEnabled(false);
            preferenceAllowRoot.setChecked(false);
            if ("root".equals(application.getMode())) {
                showDonate(true);
            }
        } else {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceAllowRoot.setEnabled(true);
        }
    }

    private void updatePlayVersion(int total) {
        if (total <= 0x0) {
            preferenceOptimizeVpn.setEnabled(false);
            preferenceOptimizeVpn.setChecked(false);
            preferenceAbnormalBack.setEnabled(false);
            preferenceAbnormalBack.setChecked(false);
            preferenceAllowRoot.setEnabled(false);
            preferenceAllowRoot.setChecked(false);
        } else if (total == 0x1) {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(false);
            preferenceAbnormalBack.setChecked(false);
            preferenceAllowRoot.setEnabled(false);
            preferenceAllowRoot.setChecked(false);
        } else if (total < BreventSettings.donateAmount()) {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceAllowRoot.setEnabled(false);
            preferenceAllowRoot.setChecked(false);
        } else {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceAllowRoot.setEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if ("brevent_about_version".equals(key)) {
            if (++repeat == 0x7) {
                getArguments().putBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, true);
                breventExperimental.addPreference(preferenceAllowRoot);
                BreventApplication application = (BreventApplication) getActivity().getApplication();
                if (!application.isPlay()) {
                    int count = mCount == 0 ? (int) application.getDonation() / 0x5 : mCount;
                    if (count < BreventSettings.donateAmount()) {
                        showDonate(false);
                    }
                }
            }
        } else if ("brevent_about_developer".equals(key)) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings",
                    "com.android.settings.DevelopmentSettings"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                UILog.d("Can't find settings", e);
            }
        }
        return false;
    }

    public void onShowDonate() {
        if (!getArguments().getBoolean(IS_PLAY, false)) {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
        }
    }

}
