package me.piebridge.brevent.ui;

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

    public static final String BREVENT_ALLOW_RECEIVER = "brevent_allow_receiver";
    public static final boolean DEFAULT_BREVENT_ALLOW_RECEIVER = false;

    public static final String IS_PLAY = "is_play";

    private PreferenceCategory breventExperimental;

    private SwitchPreference preferenceOptimizeVpn;
    private SwitchPreference preferenceAbnormalBack;
    private SwitchPreference preferenceAllowRoot;
    private SwitchPreference preferenceAllowReceiver;
    private SwitchPreference preferenceDonation;

    private Preference preferenceStandbyTimeout;

    private int repeat = 0;

    public SettingsFragment() {
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        Bundle arguments = getArguments();
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        breventExperimental = (PreferenceCategory) preferenceScreen
                .findPreference("brevent_experimental");

        preferenceOptimizeVpn = (SwitchPreference) preferenceScreen.findPreference(
                BreventConfiguration.BREVENT_OPTIMIZE_VPN);
        preferenceAbnormalBack = (SwitchPreference) preferenceScreen.findPreference(
                BreventConfiguration.BREVENT_ABNORMAL_BACK);
        preferenceAllowRoot = (SwitchPreference) preferenceScreen.findPreference(
                BreventConfiguration.BREVENT_ALLOW_ROOT);
        preferenceAllowReceiver = (SwitchPreference) preferenceScreen.findPreference(
                BREVENT_ALLOW_RECEIVER);

        preferenceDonation = (SwitchPreference) preferenceScreen.findPreference(SHOW_DONATION);

        preferenceStandbyTimeout = preferenceScreen.findPreference(
                BreventConfiguration.BREVENT_STANDBY_TIMEOUT);
        BreventApplication application = (BreventApplication) getActivity().getApplication();
        if (!application.supportStandby()) {
            ((PreferenceCategory) preferenceScreen.findPreference("brevent_list")).removePreference(
                    preferenceStandbyTimeout);
        }

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (!BuildConfig.RELEASE) {
            ((PreferenceCategory) preferenceScreen.findPreference("brevent_about"))
                    .removePreference(preferenceDonation);
        }
        if (BuildConfig.RELEASE && arguments.getBoolean(IS_PLAY, false)) {
            preferenceOptimizeVpn.setEnabled(false);
            preferenceAbnormalBack.setEnabled(false);
            preferenceAllowRoot.setEnabled(false);
            preferenceAllowReceiver.setEnabled(false);
        }
        if (!sharedPreferences.getBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, false)) {
            breventExperimental.removePreference(preferenceAllowReceiver);
            breventExperimental.removePreference(preferenceAllowRoot);
            preferenceScreen.findPreference("brevent_about_version")
                    .setOnPreferenceClickListener(this);
        }
        onUpdateBreventMethod();
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
        if ("standby_forcestop".equals(getPreferenceScreen().getSharedPreferences().getString(
                BreventConfiguration.BREVENT_METHOD, null))) {
            preferenceStandbyTimeout.setEnabled(true);
        } else {
            preferenceStandbyTimeout.setEnabled(false);
        }
    }

    private void onShowDonationChanged() {
        boolean showDonation;
        if (BuildConfig.RELEASE) {
            showDonation = getPreferenceScreen().getSharedPreferences()
                    .getBoolean(SHOW_DONATION, true);
        } else {
            showDonation = false;
        }
        ((DonateActivity) getActivity()).showDonation(showDonation);
    }

    public void updatePlayDonation(int count, int total, boolean contributor) {
        if (getActivity() == null) {
            return;
        }
        String summary;
        if (count == 1) {
            if (contributor) {
                summary = getString(R.string.show_donation_play_one_and_contributor, total);
            } else {
                summary = getString(R.string.show_donation_play_one, total);
            }
            preferenceDonation.setSummary(summary);
        } else if (count > 1) {
            if (contributor) {
                summary = getString(R.string.show_donation_play_multi_and_contributor, count, total);
            } else {
                summary = getString(R.string.show_donation_play_multi, count, total);
            }
            preferenceDonation.setSummary(summary);
        } else if (contributor) {
            preferenceDonation.setSummary(R.string.show_donation_contributor);
        }
        if (getArguments().getBoolean(IS_PLAY, false)) {
            if (contributor) {
                total += 0x5;
            }
            updatePlayVersion(total);
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
            preferenceAllowReceiver.setEnabled(false);
            preferenceAllowReceiver.setChecked(false);
        } else if (total == 0x1) {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(false);
            preferenceAbnormalBack.setChecked(false);
            preferenceAllowRoot.setEnabled(false);
            preferenceAllowRoot.setChecked(false);
            preferenceAllowReceiver.setEnabled(false);
            preferenceAllowReceiver.setChecked(false);
        } else if (total < BreventSettings.donateAmount()) {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceAllowRoot.setEnabled(false);
            preferenceAllowRoot.setChecked(false);
            preferenceAllowReceiver.setEnabled(false);
            preferenceAllowReceiver.setChecked(false);
        } else {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceAllowRoot.setEnabled(true);
            preferenceAllowReceiver.setEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if ("brevent_about_version".equals(key)) {
            if (++repeat == 0x7) {
                breventExperimental.addPreference(preferenceAllowRoot);
                breventExperimental.addPreference(preferenceAllowReceiver);
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

}
