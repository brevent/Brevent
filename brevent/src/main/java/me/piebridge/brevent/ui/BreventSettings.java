package me.piebridge.brevent.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toolbar;

import java.math.BigInteger;
import java.util.Collection;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.donation.DonateActivity;

/**
 * Settings
 * Created by thom on 2017/2/8.
 */
public class BreventSettings extends DonateActivity implements View.OnClickListener {

    private BreventConfiguration mConfiguration;

    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ColorUtils.fixToolbar(this, toolbar);
        setActionBar(toolbar);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        settingsFragment = new SettingsFragment();
        Bundle arguments = settingsFragment.getArguments();
        arguments.putBoolean(SettingsFragment.IS_PLAY, isPlay());

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.content, settingsFragment)
                .commit();

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mConfiguration = new BreventConfiguration(null, preferences);
    }

    @Override
    protected String getAlipayLink() {
        return BuildConfig.DONATE_ALIPAY;
    }

    @Override
    protected String getPaypalLink() {
        return BuildConfig.DONATE_PAYPAL;
    }

    @Override
    protected String getWechatLink() {
        return BuildConfig.DONATE_WECHAT;
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
    public void finish() {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        BreventConfiguration configuration = new BreventConfiguration(null, preferences);
        Intent data = new Intent();
        data.putExtra(Intent.ACTION_CONFIGURATION_CHANGED, mConfiguration.update(configuration));
        setResult(RESULT_OK, data);
        super.finish();
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
    public void showPlay(Collection<String> purchased) {
        if (purchased == null) {
            settingsFragment.updatePlayDonation(-1, -1, false);
        } else {
            updatePlayDonation(purchased);
        }
        super.showPlay(purchased);
    }

    private void updatePlayDonation(Collection<String> purchased) {
        int count = 0;
        int total = 0;
        boolean contributor = false;
        for (String p : purchased) {
            if (p.startsWith("contributor_")) {
                contributor = true;
            } else {
                count++;
                int i = p.indexOf('_');
                if (i > 0) {
                    String t = p.substring(i + 1);
                    if (t.length() > 0 && TextUtils.isDigitsOnly(t)) {
                        total += Integer.parseInt(t);
                    }
                }
            }
        }
        settingsFragment.updatePlayDonation(count, total, contributor);
    }

}
