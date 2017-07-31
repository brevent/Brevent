package me.piebridge.brevent.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toolbar;

import com.crashlytics.android.answers.AddToCartEvent;
import com.crashlytics.android.answers.Answers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.List;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.donation.DonateActivity;

/**
 * Settings
 * Created by thom on 2017/2/8.
 */
public class BreventSettings extends DonateActivity implements View.OnClickListener {

    static final int CONTRIBUTOR = 5;

    private BreventConfiguration mConfiguration;

    private SettingsFragment settingsFragment;

    private boolean mPlay;

    private int mTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mPlay = ((BreventApplication) getApplication()).isPlay();

        Toolbar toolbar = findViewById(R.id.toolbar);
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mConfiguration = new BreventConfiguration(preferences);
    }

    @Override
    protected String getAlipayLink() {
        return BuildConfig.DONATE_ALIPAY;
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        BreventConfiguration configuration = new BreventConfiguration(preferences);
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
    public void showPlay(@Nullable Collection<String> purchased) {
        mTotal = 0;
        if (purchased == null) {
            settingsFragment.updatePlayDonation(0, false);
        } else {
            updatePlayDonation(purchased);
        }
        super.showPlay(purchased);
    }

    @Override
    protected void onShowDonate() {
        settingsFragment.onShowDonate();
    }

    private void updatePlayDonation(Collection<String> purchased) {
        int total = 0;
        boolean contributor = false;
        for (String p : purchased) {
            if (p.startsWith("contributor_")) {
                contributor = true;
            } else {
                int i = p.indexOf('_');
                if (i > 0) {
                    String t = p.substring(i + 1);
                    if (t.length() > 0 && TextUtils.isDigitsOnly(t)) {
                        total += Integer.parseInt(t);
                    }
                }
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
        BreventApplication application = (BreventApplication) getApplication();
        boolean root = "root".equals(application.getMode());
        if (!root) {
            root = settingsFragment.getArguments()
                    .getBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, false);
        }
        int amount = root ? donateAmount() : 0x2;
        amount -= mTotal;
        if (amount > 0) {
            for (int j = 0; j < 0x5; ++j) {
                char a = (char) ('a' + j);
                skus.add("donation" + amount + a + "_" + amount);
            }
        }
        return skus;
    }

    static int donateAmount() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 0x4 : 0x3;
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
            String mode = application.getMode();
            try {
                Answers.getInstance().logAddToCart(new AddToCartEvent()
                        .putItemPrice(BigDecimal.ONE)
                        .putCurrency(Currency.getInstance("USD"))
                        .putItemName("Donate")
                        .putItemType(type)
                        .putItemId("donate-" + mode + "-" + type)
                        .putCustomAttribute("mode", mode)
                        .putCustomAttribute("installer", installer));
            } catch (IllegalStateException e) { // NOSONAR
                // do nothing
            }
        }
    }

    @Override
    public void activateDonations() {
        if (!((BreventApplication) getApplication()).isUnsafe()) {
            super.activateDonations();
        }
    }

}
