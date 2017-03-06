package me.piebridge.brevent.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
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
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String SHOW_DONATION = "show_donation";

    public static final String SHOW_ALL_APPS = "show_all_apps";
    public static final boolean DEFAULT_SHOW_ALL_APPS = false;

    public static final String SHOW_FRAMEWORK_APPS = "show_framework_apps";
    public static final boolean DEFAULT_SHOW_FRAMEWORK_APPS = false;

    public static final String HAS_PLAY = "has_play";

    private PreferenceCategory breventUi;

    private SwitchPreference donation;

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
        breventUi = (PreferenceCategory) preferenceScreen.findPreference("brevent_ui");
        donation = (SwitchPreference) preferenceScreen.findPreference(SHOW_DONATION);

        allowRoot = (SwitchPreference) preferenceScreen.findPreference(BreventConfiguration.BREVENT_ALLOW_ROOT);

        if (!BuildConfig.RELEASE) {
            donation.setEnabled(false);
            donation.setChecked(false);
            allowRoot.setEnabled(true);
        } else {
            Bundle arguments = getArguments();
            if (arguments.getBoolean(HAS_PLAY)) {
                // update later
                donation.setEnabled(false);
                allowRoot.setEnabled(false);
            } else {
                donation.setEnabled(false);
                donation.setChecked(true);
                allowRoot.setEnabled(true);
            }
        }
        breventUi.removePreference(donation);
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
            onShowDonationChanged();
        }
    }

    private void onShowDonationChanged() {
        boolean showDonation = getPreferenceScreen().getSharedPreferences().getBoolean(SHOW_DONATION, true);
        ((DonateActivity) getActivity()).showDonation(showDonation);
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
            allowRoot.setChecked(false);
        } else {
            onShowDonationChanged();
            breventUi.addPreference(donation);
            donation.setEnabled(true);
            if (total < 3) {
                allowRoot.setEnabled(false);
                allowRoot.setChecked(false);
            } else {
                allowRoot.setEnabled(true);
            }
        }
    }

}
