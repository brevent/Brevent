package me.piebridge.donation;

import android.app.Application;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.HandlerThread;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import me.piebridge.brevent.ui.AbstractActivity;
import me.piebridge.brevent.ui.PreferencesUtils;

/**
 * Donate activity, support alipay, play store
 * <p>
 * Created by thom on 2017/2/9.
 */
public abstract class DonateActivity extends AbstractActivity implements View.OnClickListener {

    public static final String PACKAGE_ALIPAY = "com.eg.android.AlipayGphone";

    public static final String PACKAGE_PLAY = "com.android.vending";

    private static final int REQUEST_PLAY_DONATE = 0x4122;

    private static final String FRAGMENT_DONATION_PROGRESS = "fragment_donation_progress";

    private static final int IAB_MAX_DONATE = 20;

    private View mDonation;
    private TextView mDonationTip;

    private PlayServiceConnection activateConnection;

    private PlayServiceConnection donateConnection;

    private List<String> mSkus;

    private volatile boolean mShowDonation = true;

    @Override
    protected void onStart() {
        super.onStart();
        if (mDonation == null) {
            mDonation = findViewById(R.id.donation);
            mDonationTip = findViewById(R.id.donation_tip);
        }
        updateDonations();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        hideDonateDialog();
    }

    @Override
    protected void onStop() {
        unbindService();
        super.onStop();
    }

    public final void updateDonations() {
        if (acceptDonation()) {
            if (!usePlayCache() || !activatePlayIfNeeded()) {
                activatePlay();
            }
            if (!isPlayInstaller()) {
                activateDonations();
            }
        } else {
            showDonation(false);
        }
    }

    protected boolean usePlayCache() {
        return true;
    }

    public final void showDonation(boolean showDonation) {
        if (!mShowDonation && showDonation) {
            activatePlay();
        }
        mShowDonation = showDonation;
        showDonation();
    }

    void showDonation() {
        mDonation.setVisibility(mShowDonation ? View.VISIBLE : View.GONE);
    }

    private static void removePlayCache(Application application) {
        PreferencesUtils.getPreferences(application).edit().remove("play").apply();
    }

    @Nullable
    public static Collection<String> getPurchased(Application application, String tag,
                                                  BigInteger modulus) {
        if (!hasPlay(application)) {
            Log.w(tag, "no play store, remove play cache");
            removePlayCache(application);
            return null;
        }
        String play = PreferencesUtils.getPreferences(application).getString("play", null);
        if (TextUtils.isEmpty(play)) {
            Log.i(tag, "no play cache: " + play);
            return null;
        }
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(play);
        } catch (JSONException e) {
            Log.d(tag, "Can't parse " + play);
            removePlayCache(application);
            return null;
        }
        List<String> data = convert(jsonArray.optJSONArray(0));
        List<String> sigs = convert(jsonArray.optJSONArray(1));
        if (data.isEmpty() || sigs.isEmpty()) {
            Log.i(tag, "no play cache: " + play);
            removePlayCache(application);
            return Collections.emptyList();
        }
        return PlayServiceConnection.checkPurchased(tag, modulus, data, sigs, true);
    }

    public boolean activatePlayIfNeeded() {
        Application application = getApplication();
        Collection<String> purchased = getPurchased(application, getTag(), getPlayModulus());
        if (purchased == null) {
            return false;
        } else {
            if (purchased.isEmpty()) {
                removePlayCache(application);
            } else {
                showPlay(purchased);
            }
            return true;
        }
    }

    @NonNull
    private static List<String> convert(JSONArray jsonArray) {
        if (jsonArray == null) {
            return Collections.emptyList();
        }
        int size = jsonArray.length();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            list.add(jsonArray.optString(i));
        }
        return list;
    }

    private synchronized void activatePlay() {
        Log.d(getTag(), "activatePlay");
        removePlayCache(getApplication());
        if (hasPlay()) {
            showPlayCheck();
            HandlerThread thread = new HandlerThread("DonateService");
            thread.start();
            unbindActivateService();
            activateConnection = new ActivatePlayServiceConnection(thread.getLooper(), this);
            Intent serviceIntent = new Intent(PlayServiceConnection.ACTION_BIND);
            serviceIntent.setPackage(PACKAGE_PLAY);
            try {
                if (!bindService(serviceIntent, activateConnection, Context.BIND_AUTO_CREATE)) {
                    unbindService(activateConnection);
                }
            } catch (IllegalArgumentException e) {
                Log.d(getTag(), "Can't bind activateConnection", e);
            }
        } else {
            showPlay(null);
        }
    }

    private void unbindActivateService() {
        if (activateConnection != null) {
            Log.d(getTag(), "unbindActivateService");
            try {
                unbindService(activateConnection);
            } catch (IllegalArgumentException e) {
                Log.d(getTag(), "Can't unbind activateConnection", e);
            }
            activateConnection = null;
        }
    }

    private void unbindDonateService() {
        if (donateConnection != null) {
            Log.d(getTag(), "unbindDonateService");
            try {
                unbindService(donateConnection);
            } catch (IllegalArgumentException e) {
                Log.d(getTag(), "Can't unbind donateConnection", e);
            }
            donateConnection = null;
        }
    }

    @Override
    @CallSuper
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.alipay) {
            donateViaAlipay();
        } else if (id == R.id.play) {
            donateViaPlay();
        }
    }

    private synchronized void donateViaPlay() {
        HandlerThread thread = new HandlerThread("DonateService");
        thread.start();
        unbindDonateService();
        donateConnection = new DonatePlayServiceConnection(thread.getLooper(), this);
        Intent serviceIntent = new Intent(PlayServiceConnection.ACTION_BIND);
        serviceIntent.setPackage(PACKAGE_PLAY);
        if (!bindService(serviceIntent, donateConnection, Context.BIND_AUTO_CREATE)) {
            unbindService(donateConnection);
        }
    }

    public void donateViaAlipay() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getAlipayLink()));
        startDonateActivity(intent, "alipay");
    }

    @Override
    @CallSuper
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PLAY_DONATE && data != null) {
            String tag = getTag();
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
            if (PlayServiceConnection.verify(tag, getPlayModulus(), purchaseData, dataSignature)) {
                activatePlay();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected void startDonateActivity(Intent intent, String type) {
        showDonateDialog();
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            hideDonateDialog();
        }
    }

    void hideDonateDialog() {
        DialogFragment fragment = (DialogFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_DONATION_PROGRESS);
        if (fragment != null && !isStopped()) {
            fragment.dismiss();
        }
    }

    private void showDonateDialog() {
        if (!isStopped()) {
            new ProgressFragment().show(getFragmentManager(), FRAGMENT_DONATION_PROGRESS);
        }
    }

    protected void activateDonations() {
        Collection<DonateItem> items = new ArrayList<>(0x3);
        if (!TextUtils.isEmpty(getAlipayLink())) {
            checkPackage(items, R.id.alipay, PACKAGE_ALIPAY);
        }
        if (items.isEmpty()) {
            mDonationTip.setText(R.string.donation_unsupported);
            mDonation.setVisibility(mShowDonation ? View.VISIBLE : View.GONE);
        } else {
            mDonationTip.setText(R.string.donation);
            new DonateTask(this, false).execute(items.toArray(new DonateItem[items.size()]));
        }
    }

    private void checkPackage(Collection<DonateItem> items, int resId, String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            DonateItem item = new DonateItem();
            item.intent = intent;
            item.imageView = findViewById(resId);
            item.imageView.setOnClickListener(this);
            if (items != null) {
                items.add(item);
            }
        }
    }

    protected abstract String getAlipayLink();

    protected abstract BigInteger getPlayModulus();

    protected boolean acceptDonation() {
        return true;
    }

    public boolean hasPlay() {
        return hasPlay(this);
    }

    static boolean hasPlay(Context context) {
        return context.getPackageManager().getLaunchIntentForPackage(PACKAGE_PLAY) != null;
    }

    protected String getApplicationId() {
        return getPackageName();
    }

    protected final boolean isPlayInstaller() {
        return hasPlay() && PACKAGE_PLAY.equals(getPackageManager()
                .getInstallerPackageName(getApplicationId()));
    }

    @CallSuper
    public void showPlay(@Nullable Collection<String> purchased) {
        unbindActivateService();
        if (purchased == null) {
            if (isPlayInstaller()) {
                mDonationTip.setText(R.string.donation_play_unavailable);
                mDonation.setVisibility(View.GONE);
            }
        } else if (canDonatePlay(purchased)) {
            Collection<DonateItem> items = new ArrayList<>(0x1);
            checkPackage(items, R.id.play, PACKAGE_PLAY);
            if (!items.isEmpty()) {
                mDonationTip.setText(R.string.donation);
                new DonateTask(this, true).execute(items.toArray(new DonateItem[items.size()]));
            }
        }
    }

    protected String getTag() {
        return "Donate";
    }

    @CallSuper
    public void showPlayCheck() {
        mDonationTip.setText(R.string.donation_play_checking);
        findViewById(R.id.play).setVisibility(View.GONE);
    }

    protected List<String> getAllSkus() {
        List<String> skus = new ArrayList<>(IAB_MAX_DONATE);
        for (int i = 1; i <= 0x4; ++i) {
            for (int j = 0; j < 0x5; ++j) {
                char a = (char) ('a' + j);
                skus.add("donation" + i + a + "_" + i);
            }
        }
        return skus;
    }

    @NonNull
    protected List<String> getDonateSkus() {
        return mSkus;
    }

    protected boolean canDonatePlay(Collection<String> purchased) {
        if (purchased.size() >= IAB_MAX_DONATE) {
            return false;
        }
        if (mSkus == null) {
            mSkus = getAllSkus();
        }
        Iterator<String> iterator = mSkus.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            if (purchased.contains(next)) {
                iterator.remove();
            }
        }
        return !mSkus.isEmpty();
    }

    public synchronized void unbindService() {
        unbindActivateService();
        unbindDonateService();
    }

    public void donatePlay(IntentSender sender) {
        try {
            startIntentSenderForResult(sender, REQUEST_PLAY_DONATE, new Intent(), 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.d(getTag(), "Can't donate");
        }
    }

    public String getSku() {
        List<String> skus = new ArrayList<>(getDonateSkus());
        for (String sku : skus) {
            if (mSkus.contains(sku)) {
                return sku;
            }
        }
        return mSkus.get(0);
    }

    static class DonateItem {
        Intent intent;
        Drawable icon;
        CharSequence label;
        ImageView imageView;
    }

}
