package me.piebridge.brevent.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
import android.widget.Toast;

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

    private PreferenceCategory preferenceBrevent;

    private SwitchPreference preferenceBackground;

    private int repeat = 0;

    private ListView mList;

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
        if (application.isFakeFramework()) {
            Preference preference = preferenceScreen.findPreference(SHOW_FRAMEWORK_APPS);
            preference.setEnabled(false);
            preference.setSummary(R.string.show_framework_apps_label_fake);
        }

        preferenceBrevent = (PreferenceCategory) preferenceScreen.findPreference("brevent");
        if (!BuildConfig.RELEASE) {
            preferenceScreen.removePreference(preferenceBrevent);
        }
        if (!application.supportAppops()) {
            preferenceBrevent.removePreference(preferenceScreen.findPreference(BREVENT_APPOPS));
        }
        preferenceBackground = (SwitchPreference) preferenceScreen
                .findPreference(BreventConfiguration.BREVENT_BACKGROUND);
        int donated = getDonated(application);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || donated < BreventSettings.CONTRIBUTOR) {
            preferenceBackground.setChecked(false);
            preferenceBrevent.removePreference(preferenceBackground);
        }
        if (BuildConfig.RELEASE) {
            updateSummaries();
            if (!getArguments().getBoolean(IS_PLAY, false)) {
                preferenceScreen.findPreference("brevent_about_version")
                        .setOnPreferenceClickListener(this);
            }
            updateDonation();
        }
        onUpdateBreventMethod();
    }

    private int getDonated(BreventApplication application) {
        if (BuildConfig.RELEASE) {
            int donation = DecimalUtils.intValue(BreventApplication.getDonation(application));
            BreventSettings activity = (BreventSettings) getActivity();
            return activity.getPlay() + donation;
        } else {
            return 0;
        }
    }

    private void updateSummaries() {
        ListAdapter rootAdapter = getPreferenceScreen().getRootAdapter();
        int size = rootAdapter.getCount();
        for (int i = 0; i < size; ++i) {
            Object item = rootAdapter.getItem(i);
            if (item instanceof Preference) {
                updatePreference((Preference) item);
            }
        }
    }

    private void updatePreference(Preference preference) {
        int recommend = getRecommend(preference);
        if (recommend > 0) {
            Context context = preference.getContext();
            BreventApplication application = (BreventApplication) context.getApplicationContext();
            CharSequence extra = application.getRecommend(context.getResources(), recommend);
            preference.setSummary(append(preference.getSummary(), extra));
            preference.setOnPreferenceChangeListener(this);
        }
    }

    private int getRecommend(Preference preference) {
        String fragment = preference.getFragment();
        if ("me.piebridge.brevent.ui.Recommend2".equals(fragment)) {
            return 0x2;
        } else if ("me.piebridge.brevent.ui.Recommend3".equals(fragment)) {
            return 0x3;
        } else {
            return 0;
        }
    }

    private CharSequence append(CharSequence summary, CharSequence extra) {
        if (summary == null) {
            return extra;
        }
        if (extra == null) {
            return summary;
        }
        if (summary.toString().contains("\n\n")) {
            return summary + "\n" + extra;
        } else {
            return summary + "\n\n" + extra;
        }
    }

    private void updateDonation() {
        BreventApplication application = (BreventApplication) getActivity().getApplication();
        double donation = BreventApplication.getDonation(application);
        if (DecimalUtils.isPositive(donation)) {
            String format = DecimalUtils.format(donation);
            String summary = getString(R.string.show_donation_rmb, format)
                    + getExtraInfo(BreventApplication.isXposed(application));
            preferenceDonation.setSummary(summary);
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
        getPreferenceScreen().findPreference("brevent_about_system")
                .setSummary(getSystemSummary());
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
        BreventSettings activity = (BreventSettings) getActivity();
        if (activity == null) {
            return;
        }
        BreventApplication application = (BreventApplication) activity.getApplication();
        String summary;
        double donation = BreventApplication.getDonation(application);
        boolean xposed = BreventApplication.isXposed(application);
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
                    summary += getExtraInfo(xposed);
                } else {
                    summary = getString(R.string.show_donation_play, play);
                }
            } else {
                if (DecimalUtils.isPositive(donation)) {
                    summary = getString(R.string.show_donation_rmb, rmb);
                    summary += getExtraInfo(xposed);
                } else if (getArguments().getBoolean(IS_PLAY, false)) {
                    summary = getString(R.string.show_donation_summary_play);
                } else {
                    summary = getString(R.string.show_donation_summary_not_play);
                }
            }
        }
        preferenceDonation.setSummary(summary);
        if (contributor) {
            total += BreventSettings.CONTRIBUTOR;
        }
        UILog.i("total: " + total + ", play: " + activity.getPlay());
        int donated = getDonated(application);
        if (total != activity.getPlay()) {
            activity.setPlay(total);
            if (total > 0) {
                Toast.makeText(application, summary, Toast.LENGTH_LONG).show();
                if (DecimalUtils.intValue(donation + total) >= activity.getRecommend()) {
                    preferenceDonation.setChecked(false);
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && donated < BreventSettings.CONTRIBUTOR
                && getDonated(application) >= BreventSettings.CONTRIBUTOR) {
            preferenceBrevent.addPreference(preferenceBackground);
        }
    }

    private String getExtraInfo(boolean xposed) {
        StringBuilder sb = new StringBuilder();
        if (xposed) {
            sb.append(getString(R.string.show_donation_xposed));
        }
        sb.append(getString(R.string.show_donation_brefoil));
        return sb.toString();
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
            ((BreventApplication) getActivity().getApplication()).launchDevelopmentSettings();
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
                UILog.i("count: " + mList.getCount() + ", position: " + position);
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
        } else {
            int recommend = getRecommend(preference);
            if (recommend > 0) {
                BreventApplication application = (BreventApplication) getActivity().getApplication();
                application.setRecommend(preference.getKey(), recommend, (boolean) newValue);
            }
        }
        return true;
    }

    private String getSystemSummary() {
        Activity activity = getActivity();
        Resources resources = activity.getResources();
        String supported = resources.getString(R.string.brevent_about_system_supported);
        String unsupported = resources.getString(R.string.brevent_about_system_unsupported);
        BreventApplication application = (BreventApplication) activity.getApplicationContext();
        return resources.getString(R.string.brevent_about_system_summary,
                application.supportStandby() ? supported : unsupported,
                application.supportStopped() ? supported : unsupported,
                application.supportAppops() ? supported : unsupported);
    }

}
