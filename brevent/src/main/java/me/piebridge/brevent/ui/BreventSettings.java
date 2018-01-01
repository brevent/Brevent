package me.piebridge.brevent.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.MenuItem;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import me.piebridge.SimpleSu;
import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.donation.DonateActivity;
import me.piebridge.stats.StatsUtils;

/**
 * Settings
 * Created by thom on 2017/2/8.
 */
public class BreventSettings extends DonateActivity {

    static final String SETTINGS_POSITION = "SETTINGS_POSITION";

    static final String DAEMON_TIME = "daemon_time";

    static final int CONTRIBUTOR = 5;

    static final int DONATE_AMOUNT = 3;

    private SettingsFragment settingsFragment;

    private boolean mPlay;

    private int mTotal;

    private static final int SIZE_1 = 30;

    private static final int SIZE_2 = 60;

    private static final int SIZE_3 = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mPlay = ((BreventApplication) getApplication()).isPlay();

        setActionBar(findViewById(R.id.toolbar));

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        settingsFragment = new SettingsFragment();
        Bundle arguments = settingsFragment.getArguments();
        arguments.putBoolean(SettingsFragment.LIKE_PLAY, mPlay);
        arguments.putBoolean(SettingsFragment.IS_PLAY, mPlay && isPlayInstaller());
        if (savedInstanceState != null) {
            arguments.putInt(SETTINGS_POSITION, savedInstanceState.getInt(SETTINGS_POSITION));
        }

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.content, settingsFragment)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(R.string.menu_settings);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        settingsFragment.updatePosition();
    }

    @Override
    protected String getAlipayLink() {
        return BuildConfig.DONATE_ALIPAY;
    }

    @Override
    protected BigInteger getPlayModulus() {
        return new BigInteger(1, BuildConfig.DONATE_PLAY);
    }

    @Override
    protected boolean acceptDonation() {
        return BuildConfig.RELEASE;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void showPlay(@Nullable Collection<String> purchased) {
        mTotal = 0;
        if (purchased == null) {
            settingsFragment.updatePlayDonation(0, false);
        } else {
            updatePlayDonation(purchased);
        }
        super.showPlay(purchased);
    }

    public static int getPlayDonation(@Nullable Collection<String> purchased) {
        if (purchased == null || purchased.isEmpty()) {
            return 0;
        }
        int total = 0;
        boolean contributor = false;
        for (String p : purchased) {
            if (p.startsWith("contributor_")) {
                contributor = true;
            } else {
                total += parse(p);
            }
        }
        if (contributor) {
            return total + CONTRIBUTOR;
        } else {
            return total;
        }
    }

    private static int parse(String p) {
        int i = p.indexOf('_');
        if (i > 0) {
            String t = p.substring(i + 1);
            if (t.length() > 0 && TextUtils.isDigitsOnly(t)) {
                return Integer.parseInt(t);
            }
        }
        return 0;
    }

    private void updatePlayDonation(@NonNull Collection<String> purchased) {
        int total = 0;
        boolean contributor = false;
        for (String p : purchased) {
            if (p.startsWith("contributor_")) {
                contributor = true;
            } else {
                total += parse(p);
            }
        }
        mTotal += total;
        if (contributor) {
            mTotal += CONTRIBUTOR;
        }
        settingsFragment.updatePlayDonation(total, contributor);
    }

    @Override
    protected boolean isPlay() {
        return mPlay;
    }

    @Override
    protected String getApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }

    @Override
    protected List<String> getDonateSkus() {
        List<String> skus = new ArrayList<>();
        int size = getIntent().getIntExtra(BreventIntent.EXTRA_BREVENT_SIZE, 0);
        int amount = getRecommend(this, size);
        amount -= mTotal;
        if (amount > 0) {
            for (int j = 0; j < 0x5; ++j) {
                char a = (char) ('a' + j);
                skus.add("donation" + amount + a + "_" + amount);
            }
        }
        return skus;
    }

    @Override
    public void startDonateActivity(Intent intent, String type) {
        super.startDonateActivity(intent, type);
        logDonate(type);
    }

    @Override
    public void donatePlay(IntentSender intentSender) {
        super.donatePlay(intentSender);
        logDonate("play");
    }

    private void logDonate(String type) {
        if (BuildConfig.RELEASE) {
            BreventApplication application = (BreventApplication) getApplication();
            String installer = application.getInstaller();
            String mode = SimpleSu.hasSu() ? "root" : "shell";
            StatsUtils.logDonate(type, mode, installer);
        }
    }

    @Override
    public void activateDonations() {
        if (!((BreventApplication) getApplication()).isUnsafe()) {
            super.activateDonations();
        }
    }

    @Override
    protected String getTag() {
        return UILog.TAG;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SETTINGS_POSITION, settingsFragment.getPosition());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected boolean usePlayCache() {
        SharedPreferences preferences = PreferencesUtils.getPreferences(this);
        long daemonTime = preferences.getLong(DAEMON_TIME, 0);
        BreventApplication application = (BreventApplication) getApplication();
        if (daemonTime != application.mDaemonTime) {
            preferences.edit().putLong(DAEMON_TIME, application.mDaemonTime).apply();
            return false;
        } else {
            return !preferences.getBoolean(SettingsFragment.SHOW_DONATION, true);
        }
    }

    private static int getRecommend(int size) {
        if (size >= SIZE_3) {
            return BreventSettings.DONATE_AMOUNT;
        } else if (size >= SIZE_2) {
            return 0x2;
        } else if (size >= SIZE_1) {
            return 0x1;
        } else {
            return 0;
        }
    }

    static int getRecommend(Activity activity, int size) {
        int recommend = BreventSettings.getRecommend(size);
        if (recommend > 0) {
            int recommend2 = PreferencesUtils.getPreferences(activity)
                    .getInt(DonationPreference.DONATION_RECOMMEND, 0);
            return Math.max(recommend, recommend2);
        } else {
            return recommend;
        }
    }

}
