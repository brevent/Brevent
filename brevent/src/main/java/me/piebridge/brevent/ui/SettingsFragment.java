package me.piebridge.brevent.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
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

    public static final String IS_PLAY = "is_play";
    private static final String LOCALE_CHANGED = "LOCALE_CHANGED";

    private SwitchPreference preferenceDonation;

    private Preference preferenceStandbyTimeout;

    private PreferenceCategory preferenceBrevent;

    private ListView mList;

    private int mDonated;

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

        preferenceDonation = (SwitchPreference) findPreference(SHOW_DONATION);

        preferenceStandbyTimeout = findPreference(BreventConfiguration.BREVENT_STANDBY_TIMEOUT);

        findPreference("brevent_language").setOnPreferenceChangeListener(this);

        BreventApplication application = (BreventApplication) getActivity().getApplication();
        if (!application.supportStandby()) {
            ((PreferenceCategory) findPreference("brevent_list"))
                    .removePreference(preferenceStandbyTimeout);
        }
        if (!application.supportUpgrade()) {
            findPreference(BreventConfiguration.BREVENT_AUTO_UPDATE).setEnabled(false);
        }
        if (application.isFakeFramework()) {
            Preference preference = findPreference(SHOW_FRAMEWORK_APPS);
            preference.setEnabled(false);
            preference.setSummary(R.string.show_framework_apps_label_fake);
        }

        preferenceBrevent = (PreferenceCategory) findPreference("brevent");
        if (!BuildConfig.RELEASE) {
            preferenceScreen.removePreference(preferenceBrevent);
        }
        if (!application.supportAppops()) {
            preferenceBrevent.removePreference(findPreference(BreventConfiguration.BREVENT_APPOPS));
        }
        if (BuildConfig.RELEASE) {
            updateSummaries();
            updateDonation();
        }
        updateSummaries(getDonated(application), false);
        onUpdateBreventMethod();
        mDonated = getDonated(application);
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
            preference.setSummary(join(preference.getSummary(), getRecommend(preference, recommend)));
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

    private void updateSummaries(int donated, boolean remove) {
        ListAdapter rootAdapter = getPreferenceScreen().getRootAdapter();
        int size = rootAdapter.getCount();
        for (int i = 0; i < size; ++i) {
            Object item = rootAdapter.getItem(i);
            if (item instanceof Preference) {
                updatePreference((Preference) item, donated, remove);
            }
        }
        BreventApplication application = (BreventApplication) getActivity().getApplication();
        if (!application.supportDisable()) {
            disable(BreventConfiguration.BREVENT_DISABLE);
        }
        if (!application.supportAppops()) {
            disable(BreventConfiguration.BREVENT_APPOPS);
            disable(BreventConfiguration.BREVENT_BACKGROUND);
        }
    }

    private void disable(String key) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setEnabled(false);
            preferenceBrevent.removePreference(preference);
        }
    }

    private void updatePreference(Preference preference, int donated, boolean remove) {
        int require = getRequire(preference);
        if (require > 0) {
            if ((remove && donated < require) || shouldRemove(preference)) {
                if (preference instanceof SwitchPreference) {
                    ((SwitchPreference) preference).setChecked(false);
                }
                preferenceBrevent.removePreference(preference);
            } else {
                preference.setEnabled(true);
                preference.setSummary(join(preference.getSummary(), getRequire(preference, require)));
            }
        }
    }

    CharSequence getRecommend(Preference preference, int recommend) {
        Resources resources = preference.getContext().getResources();
        String[] brefoils = resources.getStringArray(R.array.brefoils);
        return resources.getString(R.string.pay_brevent_recommend, brefoils[recommend - 1]);
    }

    CharSequence getRequire(Preference preference, int require) {
        Resources resources = preference.getContext().getResources();
        String[] brefoils = resources.getStringArray(R.array.brefoils);
        return resources.getString(R.string.pay_brevent_require, brefoils[require - 1]);
    }

    private boolean shouldRemove(Preference preference) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                && BreventConfiguration.BREVENT_BACKGROUND.equals(preference.getKey());
    }

    private int getRequire(Preference preference) {
        String fragment = preference.getFragment();
        if ("me.piebridge.brevent.ui.Require5".equals(fragment)) {
            return 0x5;
        } else if ("me.piebridge.brevent.ui.Require3".equals(fragment)) {
            return 0x3;
        } else {
            return 0;
        }
    }

    private CharSequence join(CharSequence summary, CharSequence extra) {
        if (summary == null) {
            return extra;
        }
        if (extra == null) {
            return summary;
        }
        if (summary.toString().endsWith(extra.toString())) {
            return summary;
        }
        return summary + "\n\n" + extra;
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

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        onShowDonationChanged();
        Preference preference = findPreference("brevent_about_developer");
        if (!SimpleSu.hasSu() && AppsDisabledFragment.isAdbRunning()) {
            preference.setSummary(R.string.brevent_about_developer_adb);
        } else {
            preference.setSummary(null);
        }
        findPreference("brevent_about_system").setSummary(getSystemSummary());
        preference.setOnPreferenceClickListener(this);
        if (getDonated((BreventApplication) getActivity().getApplication()) != mDonated) {
            getActivity().recreate();
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
        if (total != activity.getPlay()) {
            activity.setPlay(total);
            if (total > 0) {
                Toast.makeText(application, summary, Toast.LENGTH_SHORT).show();
                if (DecimalUtils.intValue(donation + total) >= activity.getRecommend()) {
                    preferenceDonation.setChecked(false);
                }
            }
        }
        updateSummaries(getDonated(application), true);
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
        if ("brevent_about_developer".equals(key)) {
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
                application.supportAppops() ? supported : unsupported,
                application.supportDisable() ? supported : unsupported);
    }

}
