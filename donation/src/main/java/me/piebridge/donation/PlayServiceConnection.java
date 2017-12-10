package me.piebridge.donation;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.util.ArraySet;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import me.piebridge.brevent.ui.PreferencesUtils;

/**
 * Created by thom on 2017/2/17.
 */
abstract class PlayServiceConnection extends Handler implements ServiceConnection {

    private static final byte[] SHA_EXPECTED = {-23, -73, -17, -27, 64, -2, -89, 121, 97, -67,
            59, -119, 71, 50, -47, -2, 119, 72, -48, 80};

    static final int MESSAGE_ACTIVATE = 0;

    static final int MESSAGE_DONATE = 1;

    private static final int MESSAGE_CHECK = 2;

    private static final int DELAY = 1000;

    private static final int VERSION = 0x3;

    private static final String TYPE = "inapp";

    static final String ACTION_BIND = "com.android.vending.billing.InAppBillingService.BIND";

    private final WeakReference<DonateActivity> mReference;

    private final String mPackageName;

    private IInAppBillingService mInApp;

    private final Object lock = new Object();

    private Handler uiHandler;

    private final int mType;

    private final String mSku;

    private final String mTag;

    PlayServiceConnection(int type, Looper looper, DonateActivity donateActivity) {
        super(looper);
        mType = type;
        mTag = donateActivity.getTag();
        mReference = new WeakReference<>(donateActivity);
        mPackageName = donateActivity.getApplicationId();
        uiHandler = new UiHandler(donateActivity);
        mSku = mType == MESSAGE_DONATE ? donateActivity.getSku() : null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        synchronized (lock) {
            mInApp = IInAppBillingService.Stub.asInterface(service);
        }
        obtainMessage(mType, mSku).sendToTarget();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        synchronized (lock) {
            mInApp = null;
        }
        getLooper().quit();
    }

    boolean isConnected() {
        synchronized (lock) {
            return mInApp != null;
        }
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case MESSAGE_ACTIVATE:
                doActivate();
                break;
            case MESSAGE_DONATE:
                doDonate((String) message.obj);
                break;
        }
    }

    private void doDonate(String sku) {
        try {
            Bundle bundle = mInApp.getBuyIntent(VERSION, mPackageName, sku, TYPE, null);
            PendingIntent intent = bundle.getParcelable("BUY_INTENT");
            DonateActivity donateActivity = mReference.get();
            if (donateActivity != null && intent != null) {
                uiHandler.obtainMessage(MESSAGE_DONATE, intent.getIntentSender()).sendToTarget();
            }
        } catch (RemoteException e) {
            Log.d(mTag, "Can't getBuyIntent", e);
        }
    }

    private void doActivate() {
        try {
            Collection<String> purchased = null;
            uiHandler.sendEmptyMessageDelayed(MESSAGE_CHECK, DELAY);
            synchronized (lock) {
                if (mInApp != null && mInApp.isBillingSupported(VERSION, mPackageName, TYPE) == 0) {
                    Bundle inapp = mInApp.getPurchases(VERSION, mPackageName, TYPE, null);
                    purchased = checkPurchased(inapp);
                }
            }
            uiHandler.removeMessages(MESSAGE_CHECK);
            uiHandler.obtainMessage(MESSAGE_ACTIVATE, purchased).sendToTarget();
        } catch (RemoteException e) {
            Log.d(mTag, "Can't check Play", e);
        }
    }

    private static boolean isEmpty(List<String> collection) {
        return collection == null || collection.isEmpty();
    }

    private Collection<String> checkPurchased(Bundle bundle) {
        List<String> data = bundle.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
        List<String> sigs = bundle.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
        DonateActivity donateActivity = mReference.get();
        if (donateActivity == null) {
            return Collections.emptyList();
        }
        JSONArray json = new JSONArray();
        json.put(new JSONArray(data));
        json.put(new JSONArray(sigs));
        PreferencesUtils.getPreferences(donateActivity)
                .edit().putString("play", json.toString()).apply();
        return checkPurchased(mTag, donateActivity.getPlayModulus(), data, sigs);
    }

    static Collection<String> checkPurchased(String tag, BigInteger modulus,
                                             List<String> data, List<String> sigs) {
        Collection<String> purchased = new ArraySet<>();
        if (isEmpty(data) || isEmpty(sigs)) {
            return purchased;
        }

        int size = data.size();
        if (size > sigs.size()) {
            size = sigs.size();
        }

        for (int i = 0; i < size; ++i) {
            String datum = data.get(i);
            if (verify(tag, modulus, datum, sigs.get(i))) {
                checkProductId(purchased, tag, datum);
            }
        }
        return purchased;
    }

    static void checkProductId(Collection<String> purchased, String tag, String datum) {
        try {
            JSONObject json = new JSONObject(datum);
            if (json.optInt("purchaseState", -1) == 0) {
                String productId = json.optString("productId");
                if (!TextUtils.isEmpty(productId)) {
                    purchased.add(productId);
                }
            }
        } catch (JSONException e) {
            Log.d(tag, "Can't check productId from " + datum);
        }
    }

    static boolean verify(String tag, BigInteger modulus, String data, String signature) {
        if (TextUtils.isEmpty(data) || TextUtils.isEmpty(signature)) {
            return false;
        }
        BigInteger exponent = BigInteger.valueOf(0x10001);
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(data.getBytes());
            byte[] digest = sha1.digest();
            byte[] key = Base64.decode(signature, Base64.DEFAULT);
            byte[] sign = new BigInteger(1, key).modPow(exponent, modulus).toByteArray();
            for (int i = digest.length - 1, j = sign.length - 1; i >= 0; --i, --j) {
                sign[j] ^= digest[i];
            }
            sha1.reset();
            sha1.update(sign);
            digest = sha1.digest();
            for (int i = digest.length - 1; i >= 0; --i) {
                if (digest[i] != SHA_EXPECTED[i]) {
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            Log.d(tag, "Can't verify");
        }
        return false;
    }

    private static class UiHandler extends Handler {

        private final WeakReference<DonateActivity> mReference;

        UiHandler(DonateActivity donateActivity) {
            super(donateActivity.getMainLooper());
            mReference = new WeakReference<>(donateActivity);
        }

        @Override
        public void handleMessage(Message message) {
            DonateActivity donateActivity = mReference.get();
            if (donateActivity != null) {
                switch (message.what) {
                    case MESSAGE_ACTIVATE:
                        donateActivity.showPlay((Collection<String>) message.obj);
                        break;
                    case MESSAGE_DONATE:
                        IntentSender sender = (IntentSender) message.obj;
                        donateActivity.donatePlay(sender);
                        break;
                    case MESSAGE_CHECK:
                        donateActivity.showPlayCheck();
                        break;
                }
            }
        }

    }
}
