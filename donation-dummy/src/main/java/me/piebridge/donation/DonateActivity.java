package me.piebridge.donation;

import android.app.Application;
import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.Nullable;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import me.piebridge.brevent.ui.AbstractActivity;

/**
 * Created by thom on 2017/12/10.
 */
public abstract class DonateActivity extends AbstractActivity {

    @Nullable
    public static Collection<String> getPurchased(Application application, String tag,
                                                  BigInteger modulus) {
        return null;
    }

    public void showDonation(boolean showDonation) {

    }

    protected final boolean isPlayInstaller() {
        return false;
    }

    protected abstract String getAlipayLink();

    protected abstract BigInteger getPlayModulus();

    protected abstract boolean acceptDonation();

    public void showPlay(@Nullable Collection<String> purchased) {

    }

    protected abstract boolean isPlay();

    protected abstract String getApplicationId();

    protected abstract List<String> getDonateSkus();

    public void donateViaAlipay() {

    }

    public void startDonateActivity(Intent intent, String type) {

    }

    public void donatePlay(IntentSender intentSender) {

    }

    public void activateDonations() {

    }

    protected abstract String getTag();

    protected abstract boolean usePlayCache();

}
