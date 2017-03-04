package me.piebridge.brevent.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
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
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String SHOW_DONATION = "show_donation";

    public static final String SHOW_ALL_APPS = "show_all_apps";
    public static final boolean DEFAULT_SHOW_ALL_APPS = false;

    public static final String SHOW_FRAMEWORK_APPS = "show_framework_apps";
    public static final boolean DEFAULT_SHOW_FRAMEWORK_APPS = false;

    public static final String HAS_PLAY = "has_play";

    private SwitchPreference donation;

    private SwitchPreference allowGcm;

    private SwitchPreference allowRoot;

    public SettingsFragment() {
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);


        PreferenceScreen preferenceScreen = getPreferenceScreen();
        donation = (SwitchPreference) preferenceScreen.findPreference(SHOW_DONATION);
        donation.setSummary(null);

        allowGcm = (SwitchPreference) preferenceScreen.findPreference(BreventConfiguration.BREVENT_ALLOW_GCM);
        allowRoot = (SwitchPreference) preferenceScreen.findPreference(BreventConfiguration.BREVENT_ALLOW_ROOT);
        allowRoot.setEnabled(true);
        allowGcm.setEnabled(true);

        if (!BuildConfig.RELEASE) {
            donation.setEnabled(false);
            donation.setChecked(false);
        } else {
            donation.setEnabled(false);
            donation.setChecked(true);
            if (getArguments().getBoolean(HAS_PLAY)) {
                allowGcm.setEnabled(false);
                allowRoot.setEnabled(false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SHOW_DONATION.equals(key)) {
            ((DonateActivity) getActivity()).updateDonations();
        }
    }

    public void updatePlayDonation(int count, int total) {
        String summary;
        if (count == 1) {
            summary = getString(R.string.show_donation_play_one, total);
            donation.setSummary(summary);
        } else if (count > 1) {
            summary = getString(R.string.show_donation_play_multi, count, total);
            donation.setSummary(summary);
        }
        if (total == 0) {
            donation.setEnabled(false);
            allowGcm.setChecked(false);
            allowRoot.setChecked(false);
        } else {
            donation.setEnabled(true);
            if (total < 3) {
                allowGcm.setEnabled(true);
                allowRoot.setEnabled(false);
                allowRoot.setChecked(false);
            } else {
                allowGcm.setEnabled(true);
                allowRoot.setEnabled(true);
            }
        }
    }

}
