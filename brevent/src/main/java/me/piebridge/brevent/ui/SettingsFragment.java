package me.piebridge.brevent.ui;

import android.app.Activity;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Objects;

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

    public static final String LIKE_PLAY = "like_play";
    public static final String IS_PLAY = "is_play";

    private static final String FRAGMENT_DONATE = "donate";

    private SwitchPreference preferenceOptimizeVpn;
    private SwitchPreference preferenceAbnormalBack;
    private SwitchPreference preferenceOptimizeAudio;
    private SwitchPreference preferenceAllowRoot;
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

        preferenceOptimizeVpn = (SwitchPreference) preferenceScreen
                .findPreference(BreventConfiguration.BREVENT_OPTIMIZE_VPN);
        preferenceAbnormalBack = (SwitchPreference) preferenceScreen
                .findPreference(BreventConfiguration.BREVENT_ABNORMAL_BACK);
        preferenceOptimizeAudio = (SwitchPreference) preferenceScreen
                .findPreference(BreventConfiguration.BREVENT_OPTIMIZE_AUDIO);
        preferenceAllowRoot = (SwitchPreference) preferenceScreen
                .findPreference(BreventConfiguration.BREVENT_ALLOW_ROOT);

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
            preferenceAutoUpdate.setChecked(false);
            preferenceAutoUpdate.setEnabled(false);
        }

        if (BuildConfig.RELEASE) {
            if (!isDeprecated()) {
                preferenceOptimizeVpn.setEnabled(false);
                preferenceAbnormalBack.setEnabled(false);
                preferenceOptimizeAudio.setEnabled(false);
                preferenceAllowRoot.setEnabled(false);
            }
            preferenceScreen.findPreference("network_adb")
                    .setSummary(getFingerPrint(BuildConfig.ADB_K));
        } else {
            preferenceScreen.removePreference(preferenceScreen.findPreference("brevent"));
            preferenceOptimizeVpn.setSummary(R.string.brevent_optimize_vpn_label_debug);
            preferenceAbnormalBack.setSummary(R.string.brevent_abnormal_back_label_debug);
            preferenceOptimizeAudio.setSummary(R.string.brevent_optimize_audio_label_debug);
            preferenceAllowRoot.setSummary(R.string.brevent_allow_root_label_debug);
        }
        if (!AppsDisabledFragment.hasRoot()) {
            ((PreferenceCategory) preferenceScreen.findPreference("brevent_experimental"))
                    .removePreference(preferenceAllowRoot);
        }
        if (BuildConfig.RELEASE) {
            if (!getArguments().getBoolean(IS_PLAY, false)) {
                preferenceScreen.findPreference("brevent_about_version")
                        .setOnPreferenceClickListener(this);
            }
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
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceOptimizeAudio.setEnabled(true);
        } else if (getArguments().getBoolean(IS_PLAY, false)) {
            preferenceDonation.setSummary(null);
        } else {
            preferenceDonation.setSummary(R.string.show_donation_summary_not_play);
        }
        if (isDeprecated() || DecimalUtils.intValue(donation) >= BreventSettings.DONATE_AMOUNT) {
            preferenceAllowRoot.setEnabled(true);
        } else if (!application.hasPlay()) {
            preferenceAllowRoot.setEnabled(false);
            preferenceAllowRoot.setChecked(false);
        }
    }

    static boolean isDeprecated() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mList = view.findViewById(android.R.id.list);
        return view;
    }

    public void showDonate(boolean root) {
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
        fragment.setRoot(root);
        fragment.show(getFragmentManager(), FRAGMENT_DONATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        onShowDonationChanged();
        Preference preference = getPreferenceScreen().findPreference("brevent_about_developer");
        if (!AppsDisabledFragment.hasRoot() && AppsDisabledFragment.isAdbRunning()) {
            preference.setSummary(R.string.brevent_about_developer_adb);
        } else {
            preference.setSummary(null);
        }
        preference.setOnPreferenceClickListener(this);
        if (BuildConfig.RELEASE) {
            Activity activity = getActivity();
            double donation = BreventApplication.getDonation(activity);
            int playDonation = BreventApplication.getPlayDonation(activity);
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
                    summary = null;
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
        if (isDeprecated()) {
            count += BreventSettings.DONATE_AMOUNT;
        }
        if (getArguments().getBoolean(LIKE_PLAY, false)) {
            updatePlayVersion(count);
        } else if (count < BreventSettings.DONATE_AMOUNT) {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceOptimizeAudio.setEnabled(true);
            preferenceAllowRoot.setEnabled(false);
            preferenceAllowRoot.setChecked(false);
        } else {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceOptimizeAudio.setEnabled(true);
            preferenceAllowRoot.setEnabled(true);
        }
    }

    private void updatePlayVersion(int total) {
        if (total <= 0x0) {
            preferenceOptimizeVpn.setEnabled(false);
            preferenceOptimizeVpn.setChecked(false);
            preferenceAbnormalBack.setEnabled(false);
            preferenceAbnormalBack.setChecked(false);
            preferenceOptimizeAudio.setEnabled(false);
            preferenceOptimizeAudio.setChecked(false);
            preferenceAllowRoot.setEnabled(false);
            preferenceAllowRoot.setChecked(false);
        } else if (total < 0x2) {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(false);
            preferenceAbnormalBack.setChecked(false);
            preferenceOptimizeAudio.setEnabled(false);
            preferenceOptimizeAudio.setChecked(false);
            preferenceAllowRoot.setEnabled(false);
            preferenceAllowRoot.setChecked(false);
        } else if (total < BreventSettings.DONATE_AMOUNT) {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceOptimizeAudio.setEnabled(true);
            preferenceAllowRoot.setEnabled(false);
        } else {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceOptimizeAudio.setEnabled(true);
            preferenceAllowRoot.setEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (BuildConfig.RELEASE && "brevent_about_version".equals(key)) {
            if (++repeat == 0x7) {
                if (!getArguments().getBoolean(IS_PLAY, false)) {
                    showDonate(AppsDisabledFragment.hasRoot());
                }
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

    public void onShowDonate() {
        if (!getArguments().getBoolean(LIKE_PLAY, false)) {
            preferenceOptimizeVpn.setEnabled(true);
            preferenceAbnormalBack.setEnabled(true);
            preferenceOptimizeAudio.setEnabled(true);
        }
    }

    private static String getFingerPrint(byte[] key) {
        if (key == null) {
            return null;
        }
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (GeneralSecurityException e) {
            UILog.e("md5", e);
            return null;
        }
        byte[] digest = md5.digest(key);
        StringBuilder sb = new StringBuilder();
        String hex = "0123456789ABCDEF";
        int i = 0;
        int length = digest.length;
        while (true) {
            sb.append(hex.charAt((digest[i] >> 4) & 0xf));
            sb.append(hex.charAt(digest[i] & 0xf));
            if (++i < length) {
                sb.append(":");
            } else {
                break;
            }
        }
        return sb.toString();
    }

    int getPosition() {
        return mList != null ? mList.getLastVisiblePosition() : 0;
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
                activity.recreate();
            }
        }
        return true;
    }

}
