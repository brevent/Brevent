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
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by thom on 2017/2/17.
 */

public class PlayServiceConnection extends Handler implements ServiceConnection {

    static final int MESSAGE_ACTIVATE = 0;

    static final int MESSAGE_DONATE = 1;

    static final int PLAY_VERSION = 0x3;

    static final String PLAY_TYPE = "inapp";

    static final String ACTION_BIND = "com.android.vending.billing.InAppBillingService.BIND";

    private static final String TAG = "Donate";

    private final WeakReference<DonateActivity> mReference;

    private final String mPackageName;

    private IInAppBillingService mService;

    private Handler uiHandler;

    private final int mType;

    private final String mSku;

    public PlayServiceConnection(int type, Looper looper, DonateActivity donateActivity) {
        super(looper);
        mType = type;
        mReference = new WeakReference<>(donateActivity);
        mPackageName = donateActivity.getPackageName();
        uiHandler = new UiHandler(donateActivity);
        mSku = mType == MESSAGE_DONATE ? donateActivity.getSku() : null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IInAppBillingService.Stub.asInterface(service);
        obtainMessage(mType, mSku).sendToTarget();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
        getLooper().quit();
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
            Bundle bundle = mService.getBuyIntent(PLAY_VERSION, mPackageName, sku, PLAY_TYPE, null);
            PendingIntent intent = bundle.getParcelable("BUY_INTENT");
            DonateActivity donateActivity = mReference.get();
            if (donateActivity != null && intent != null) {
                uiHandler.obtainMessage(MESSAGE_DONATE, intent.getIntentSender()).sendToTarget();
            }
        } catch (RemoteException e) {
            Log.d(TAG, "Can't getBuyIntent", e);
        }
    }

    private void doActivate() {
        try {
            Collection<String> purchased = null;
            if (mService.isBillingSupported(PLAY_VERSION, mPackageName, PLAY_TYPE) == 0) {
                Bundle inapp = mService.getPurchases(PLAY_VERSION, mPackageName, PLAY_TYPE, null);
                purchased = checkPurchased(inapp);
            }
            uiHandler.obtainMessage(MESSAGE_ACTIVATE, purchased).sendToTarget();
        } catch (RemoteException e) {
            Log.d(TAG, "Can't check Play", e);
        }
    }

    private static boolean isEmpty(Collection<String> collection) {
        return collection == null || collection.isEmpty();
    }

    private Collection<String> checkPurchased(Bundle bundle) {
        Collection<String> purchased = new ArrayList<>();

        List<String> dataList = bundle.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
        List<String> signatureList = bundle.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");

        if (isEmpty(dataList) || isEmpty(signatureList)) {
            return purchased;
        }

        int size = dataList.size();
        if (size > signatureList.size()) {
            size = signatureList.size();
        }

        BigInteger modulus = mReference.get().getPlayModulus();
        for (int i = 0; i < size; ++i) {
            String s = dataList.get(i);
            if (verify(modulus, s, signatureList.get(i))) {
                String productId = checkProductId(s);
                if (productId != null) {
                    purchased.add(productId);
                }
            }
        }
        return purchased;
    }

    private String checkProductId(String s) {
        try {
            JSONObject json = new JSONObject(s);
            if (mPackageName.equals(json.optString("packageName")) &&
                    json.optInt("purchaseState", -1) == 0) {
                return json.optString("productId");
            }
        } catch (JSONException e) {
            Log.d(TAG, "Can't check productId from " + s);
        }
        return null;
    }

    static boolean verify(BigInteger modulus, String data, String signature) {
        if (TextUtils.isEmpty(data) || TextUtils.isEmpty(signature)) {
            return false;
        }
        BigInteger exponent = BigInteger.valueOf(0x10001);
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(data.getBytes());
            byte[] plain = getSignature(sha1.digest());
            byte[] key = Base64.decode(signature, Base64.DEFAULT);
            byte[] sign = new BigInteger(1, key).modPow(exponent, modulus).toByteArray();
            return equals(plain, sign);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            Log.d(TAG, "Can't verify");
        }
        return false;
    }

    private static boolean equals(byte[] a, byte[] b) { // NOSONAR
        int length = a.length;
        if (length != b.length) {
            return false;
        }

        for (int i = 0; i < length; ++i) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] getSignature(byte[] sha1) {
        // rfc3447, sha-1
        byte[] algorithm = {0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02,
                0x1a, 0x05, 0x00, 0x04, 0x14};
        ByteArrayOutputStream signature = new ByteArrayOutputStream(0xff);
        signature.write(0x01);
        // 0xff - 2 - algorithm - sha1
        for (int i = 0; i < 0xda; ++i) {
            signature.write(0xff);
        }
        signature.write(0x00);
        signature.write(algorithm, 0, algorithm.length);
        signature.write(sha1, 0, sha1.length);
        return signature.toByteArray();
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
                        @SuppressWarnings("unchecked")
                        Collection<String> purchased = (Collection<String>) message.obj;
                        donateActivity.showPlay(purchased);
                        break;
                    case MESSAGE_DONATE:
                        IntentSender sender = (IntentSender) message.obj;
                        donateActivity.donatePlay(sender);
                        break;
                }
            }
        }

    }
}
