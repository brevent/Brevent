package me.piebridge.brevent.ui;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.Objects;

import me.piebridge.SimpleSu;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.donation.DonateActivity;

/**
 * Created by thom on 2017/2/8.
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    public static final String SHOW_DONATION = "show_donation";

    public static final String SHOW_ALL_APPS = "show_all_apps";
    public static final boolean DEFAULT_SHOW_ALL_APPS = false;

    public static final String SHOW_FRAMEWORK_APPS = "show_framework_apps";
    public static final boolean DEFAULT_SHOW_FRAMEWORK_APPS = false;

    public static final String BREVENT_APPOPS = "brevent_appops";

    public static final String LIKE_PLAY = "like_play";
    public static final String IS_PLAY = "is_play";
    private static final String LOCALE_CHANGED = "LOCALE_CHANGED";

    private static final String FRAGMENT_DONATE = "donate";

    private SwitchPreference preferenceDonation;

    private Preference preferenceStandbyTimeout;

    private int repeat = 0;

    private ListView mList;

    private String mAmount;

    public SettingsFragment() {
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getPreferenceManager().setStorageDeviceProtected();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        PreferenceScreen preferenceScreen = getPreferenceScreen();

        preferenceDonation = (SwitchPreference) preferenceScreen.findPreference(SHOW_DONATION);

        preferenceStandbyTimeout = preferenceScreen
                .findPreference(BreventConfiguration.BREVENT_STANDBY_TIMEOUT);

        preferenceScreen.findPreference("brevent_language")
                .setOnPreferenceChangeListener(this);

        BreventApplication application = (BreventApplication) getActivity().getApplication();
        if (!application.supportStandby()) {
            ((PreferenceCategory) preferenceScreen.findPreference("brevent_list"))
                    .removePreference(preferenceStandbyTimeout);
        }
        if (!application.supportUpgrade()) {
            SwitchPreference preferenceAutoUpdate = (SwitchPreference) preferenceScreen
                    .findPreference(BreventConfiguration.BREVENT_AUTO_UPDATE);
            preferenceAutoUpdate.setEnabled(false);
        }

        if (!BuildConfig.RELEASE) {
            preferenceScreen.removePreference(preferenceScreen.findPreference("brevent"));
        }
        if (!application.supportAppops()) {
            ((PreferenceCategory) preferenceScreen.findPreference("brevent"))
                    .removePreference(preferenceScreen.findPreference(BREVENT_APPOPS));
        }
        if (BuildConfig.RELEASE) {
            preferenceScreen.findPreference("brevent_about_version")
                    .setOnPreferenceClickListener(this);
            updateDonation();
        }
        onUpdateBreventMethod();
    }

    private void updateDonation() {
        BreventApplication application = (BreventApplication) getActivity().getApplication();
        double donation = BreventApplication.getDonation(application);
        if (DecimalUtils.isPositive(donation)) {
            String format = DecimalUtils.format(donation);
            preferenceDonation.setSummary(getString(R.string.show_donation_rmb, format));
        } else if (getArguments().getBoolean(IS_PLAY, false)) {
            preferenceDonation.setSummary(R.string.show_donation_summary_play);
        } else {
            preferenceDonation.setSummary(R.string.show_donation_summary_not_play);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mList = view.findViewById(android.R.id.list);
        return view;
    }

    public void showDonate() {
        if (Log.isLoggable(UILog.TAG, Log.DEBUG)) {
            UILog.d("show " + FRAGMENT_DONATE);
        }
        DonateActivity activity = (DonateActivity) getActivity();
        if (activity == null || activity.isStopped()) {
            return;
        }
        AppsDonateFragment fragment = (AppsDonateFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_DONATE);
        if (fragment != null) {
            fragment.dismiss();
        }
        fragment = new AppsDonateFragment();
        fragment.show(getFragmentManager(), FRAGMENT_DONATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        onShowDonationChanged();
        Preference preference = getPreferenceScreen().findPreference("brevent_about_developer");
        if (!SimpleSu.hasSu() && AppsDisabledFragment.isAdbRunning()) {
            preference.setSummary(R.string.brevent_about_developer_adb);
        } else {
            preference.setSummary(null);
        }
        preference.setOnPreferenceClickListener(this);
        if (BuildConfig.RELEASE) {
            Activity activity = getActivity();
            BreventApplication application = (BreventApplication) activity.getApplication();
            double donation = BreventApplication.getDonation(application);
            int playDonation = BreventApplication.getPlayDonation(application);
            String amount = DecimalUtils.format(donation + playDonation);
            if (mAmount == null) {
                mAmount = amount;
            } else if (!Objects.equals(mAmount, amount)) {
                activity.recreate();
            }
        }
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
        double donation = BreventApplication.getDonation(application);
        String play = Integer.toString(total);
        String rmb = DecimalUtils.format(donation);
        if (contributor) {
            if (total > 0) {
                if (DecimalUtils.isPositive(donation)) {
                    summary = getString(R.string.show_donation_play_and_rmb_and_contributor,
                            play, rmb);
                } else {
                    summary = getString(R.string.show_donation_play_and_contributor, play);
                }
            } else {
                if (DecimalUtils.isPositive(donation)) {
                    summary = getString(R.string.show_donation_rmb_and_contributor, rmb);
                } else {
                    summary = getString(R.string.show_donation_contributor);
                }
            }

        } else {
            if (total > 0) {
                if (DecimalUtils.isPositive(donation)) {
                    summary = getString(R.string.show_donation_play_and_rmb,
                            play, rmb);
                } else {
                    summary = getString(R.string.show_donation_play, play);
                }
            } else {
                if (DecimalUtils.isPositive(donation)) {
                    summary = getString(R.string.show_donation_rmb, rmb);
                } else if (getArguments().getBoolean(IS_PLAY, false)) {
                    summary = getString(R.string.show_donation_summary_play);
                } else {
                    summary = getString(R.string.show_donation_summary_not_play);
                }
            }
        }
        preferenceDonation.setSummary(summary);
        int count = total + DecimalUtils.intValue(donation);
        if (contributor) {
            count += BreventSettings.CONTRIBUTOR;
        }
        ListAdapter rootAdapter = getPreferenceScreen().getRootAdapter();
        int size = rootAdapter.getCount();
        for (int i = 0; i < size; ++i) {
            Object item = rootAdapter.getItem(i);
            if (item instanceof DonationPreference) {
                ((DonationPreference) item).updateDonated(count);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (BuildConfig.RELEASE && "brevent_about_version".equals(key)) {
            if (++repeat == 0x7) {
                showDonate();
                repeat = 0;
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

    int getPosition() {
        Bundle arguments = getArguments();
        if (arguments.getBoolean(LOCALE_CHANGED, false)) {
            arguments.putBoolean(LOCALE_CHANGED, false);
            return mList != null ? mList.getLastVisiblePosition() : 0;
        } else {
            return -1;
        }
    }

    void updatePosition() {
        if (mList != null) {
            int position = getArguments().getInt(BreventSettings.SETTINGS_POSITION, 0);
            if (position > 0 && position < mList.getCount()) {
                UILog.d("count: " + mList.getCount() + ", position: " + position);
                mList.smoothScrollToPosition(position);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ("brevent_language".equals(preference.getKey())) {
            Activity activity = getActivity();
            String language = String.valueOf(newValue);
            if ("auto".equals(language)) {
                language = "";
            }
            if (LocaleUtils.setOverrideLanguage(activity, language)) {
                getArguments().putBoolean(LOCALE_CHANGED, true);
                activity.recreate();
            }
        }
        return true;
    }

}
