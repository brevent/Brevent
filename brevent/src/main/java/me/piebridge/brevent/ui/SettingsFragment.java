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

import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.brevent.protocol.BreventUtils;
import me.piebridge.donation.DonateActivity;

/**
 * Created by thom on 2017/2/8.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    public static final String SHOW_DONATION = "show_donation";

    public static final String SHOW_ALL_APPS = "show_all_apps";
    public static final boolean DEFAULT_SHOW_ALL_APPS = false;

    public static final String SHOW_FRAMEWORK_APPS = "show_framework_apps";
    public static final boolean DEFAULT_SHOW_FRAMEWORK_APPS = false;

    public static final String HAS_PLAY = "has_play";
    public static final String IS_PLAY = "is_play";

    private PreferenceCategory breventAdvanced;
    private PreferenceCategory breventUi;

    private SwitchPreference preferenceDonation;

    private SwitchPreference preferenceAllowGcm;
    private SwitchPreference preferenceAllowRoot;

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

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        breventAdvanced = (PreferenceCategory) preferenceScreen.findPreference("brevent_advanced");
        breventUi = (PreferenceCategory) preferenceScreen.findPreference("brevent_ui");
        preferenceDonation = (SwitchPreference) preferenceScreen.findPreference(SHOW_DONATION);

        preferenceAllowGcm = (SwitchPreference) preferenceScreen.findPreference(BreventConfiguration.BREVENT_ALLOW_GCM);
        preferenceAllowRoot = (SwitchPreference) preferenceScreen.findPreference(BreventConfiguration.BREVENT_ALLOW_ROOT);

        preferenceStandbyTimeout = preferenceScreen.findPreference(BreventConfiguration.BREVENT_STANDBY_TIMEOUT);
        if (!BreventUtils.supportStandby()) {
            ((PreferenceCategory) preferenceScreen.findPreference("brevent_list")).removePreference(preferenceStandbyTimeout);
        }

        if (getArguments().getBoolean(IS_PLAY, false)) {
            preferenceAllowGcm.setEnabled(false);
            preferenceAllowRoot.setEnabled(false);
        } else {
            if (!getArguments().getBoolean(HAS_PLAY, false)) {
                preferenceDonation.setChecked(true);
            }
            removeDonationIfNeeded();
        }
        if (!getPreferenceScreen().getSharedPreferences().getBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, false)) {
            breventAdvanced.removePreference(preferenceAllowRoot);
            preferenceScreen.findPreference("brevent_about_version").setOnPreferenceClickListener(this);
        }
        if (getArguments().getInt(Intent.EXTRA_ALARM_COUNT, 0) == 0) {
            breventAdvanced.removePreference(preferenceScreen.findPreference(BreventConfiguration.BREVENT_ALLOW_GCM));
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
        if ("standby_forcestop".equals(getPreferenceScreen().getSharedPreferences().getString(BreventConfiguration.BREVENT_METHOD, null))) {
            preferenceStandbyTimeout.setEnabled(true);
        } else {
            preferenceStandbyTimeout.setEnabled(false);
        }
    }

    private void onShowDonationChanged() {
        boolean showDonation = getPreferenceScreen().getSharedPreferences().getBoolean(SHOW_DONATION, true);
        ((DonateActivity) getActivity()).showDonation(showDonation);
    }

    public void updatePlayDonation(int count, int total, boolean contributor) {
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
        if (contributor) {
            total += 0x5;
        }
        if (total <= 0) {
            preferenceDonation.setChecked(true);
            onShowDonationChanged();
            removeDonationIfNeeded();
            if (getArguments().getBoolean(IS_PLAY, false) && total == 0) {
                preferenceAllowGcm.setEnabled(false);
                preferenceAllowGcm.setChecked(false);
                preferenceAllowRoot.setEnabled(false);
                preferenceAllowRoot.setChecked(false);
            } else {
                preferenceAllowGcm.setEnabled(true);
                preferenceAllowRoot.setEnabled(true);
            }
        } else {
            breventUi.addPreference(preferenceDonation);
            if (total == 1) {
                preferenceAllowGcm.setEnabled(false);
                preferenceAllowGcm.setChecked(false);
                preferenceAllowRoot.setEnabled(false);
                preferenceAllowRoot.setChecked(false);
            } else if (total == 2) {
                preferenceAllowGcm.setEnabled(true);
                preferenceAllowRoot.setEnabled(false);
                preferenceAllowRoot.setChecked(false);
            } else {
                preferenceAllowGcm.setEnabled(true);
                preferenceAllowRoot.setEnabled(true);
            }
        }
    }

    private void removeDonationIfNeeded() {
        if (!"colombo".equals(Build.DEVICE)) {
            breventUi.removePreference(preferenceDonation);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if ("brevent_about_version".equals(key)) {
            if (++repeat == 0x7) {
                breventAdvanced.addPreference(preferenceAllowRoot);
            }
        } else if ("brevent_about_developer".equals(key)) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.DevelopmentSettings"));
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
