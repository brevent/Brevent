package me.piebridge.brevent.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventConfiguration;
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

    private PreferenceCategory breventAdvanced;

    private SwitchPreference preferenceDonation;

    private SwitchPreference preferenceAllowRoot;

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
        preferenceDonation = (SwitchPreference) preferenceScreen.findPreference(SHOW_DONATION);
        preferenceAllowRoot = (SwitchPreference) preferenceScreen.findPreference(BreventConfiguration.BREVENT_ALLOW_ROOT);

        if (!getPreferenceScreen().getSharedPreferences().getBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, false)) {
            breventAdvanced.removePreference(preferenceAllowRoot);
            preferenceScreen.findPreference("brevent_about_version").setOnPreferenceClickListener(this);
        }
        if (getArguments().getInt(Intent.EXTRA_ALARM_COUNT, 0) == 0) {
            breventAdvanced.removePreference(preferenceScreen.findPreference(BreventConfiguration.BREVENT_ALLOW_GCM));
            breventAdvanced.removePreference(preferenceScreen.findPreference(BreventConfiguration.BREVENT_OPTIMIZE_MM_GCM));
        } else if (getContext().getPackageManager().getLaunchIntentForPackage("com.tencent.mm") == null) {
            breventAdvanced.removePreference(preferenceScreen.findPreference(BreventConfiguration.BREVENT_OPTIMIZE_MM_GCM));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        onShowDonationChanged();
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
            preferenceDonation.setSummary(summary);
        } else if (count > 1) {
            summary = getString(R.string.show_donation_play_multi, count, total);
            preferenceDonation.setSummary(summary);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (++repeat == 0x7) {
            breventAdvanced.addPreference(preferenceAllowRoot);
        }
        return false;
    }

}
