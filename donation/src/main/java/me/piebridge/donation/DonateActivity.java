package me.piebridge.donation;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Process;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Donate activity, support alipay, wechat, play store
 * <p>
 * Created by thom on 2017/2/9.
 */
public abstract class DonateActivity extends Activity implements View.OnClickListener {

    public static final String PACKAGE_ALIPAY = "com.eg.android.AlipayGphone";

    public static final String PACKAGE_WECHAT = "com.tencent.mm";

    public static final String PACKAGE_PLAY = "com.android.vending";

    private static final int REQUEST_WECHAT_DONATE_SDA = 0x4121;

    private static final int REQUEST_PLAY_DONATE = 0x4122;

    private static final int PERMISSION_WECHAT_DONATE = 0x4123;

    private static final String KEY_WECHAT_DONATE_SDA = "donation.wechat.sda";
    private static final String KEY_WECHAT_DONATE_URI = "donation.wechat.uri";

    private static final String WECHAT_DONATE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final String FRAGMENT_DONATION_WECHAT = "fragment_donation_wechat";

    private static final String FRAGMENT_DONATION_PROGRESS = "fragment_donation_progress";

    private static final int IAB_MAX_DONATE = 20;

    private static final String TAG = "Donate";

    private View mDonation;
    private TextView mDonationTip;

    private PlayServiceConnection activateConnection;

    private PlayServiceConnection donateConnection;

    private List<String> mSkus;

    private boolean stopped;

    private volatile boolean mShowDonation = true;

    private boolean canDonate;

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
        stopped = false;
        hideDonateDialog();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        stopped = true;
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        stopped = true;
        super.onStop();
    }

    public final void updateDonations() {
        if (acceptDonation()) {
            activatePlay();
            if (!isPlay()) {
                activateDonations();
            }
        } else {
            showDonation(false);
        }
    }

    public final void showDonation(boolean showDonation) {
        mShowDonation = showDonation;
        showDonation();
    }

    void showDonation() {
        mDonation.setVisibility(mShowDonation ? View.VISIBLE : View.GONE);
    }

    private synchronized void activatePlay() {
        if (hasPlay()) {
            showPlayCheck();
            HandlerThread thread = new HandlerThread("DonateService");
            thread.start();
            unbindActivateService();
            activateConnection = new PlayServiceConnection(PlayServiceConnection.MESSAGE_ACTIVATE,
                    thread.getLooper(), this);
            Intent serviceIntent = new Intent(PlayServiceConnection.ACTION_BIND);
            serviceIntent.setPackage(PACKAGE_PLAY);
            if (!bindService(serviceIntent, activateConnection, Context.BIND_AUTO_CREATE)) {
                unbindService(activateConnection);
                activateConnection = null;
            }
        } else if (isPlay()) {
            showPlay(null);
        }
    }

    private void unbindActivateService() {
        if (activateConnection != null) {
            if (activateConnection.isConnected()) {
                try {
                    unbindService(activateConnection);
                } catch (IllegalArgumentException e) {
                    Log.d(TAG, "cannot unbind activateConnection", e);
                }
            }
            activateConnection = null;
        }
    }

    private void unbindDonateService() {
        if (donateConnection != null) {
            if (donateConnection.isConnected()) {
                try {
                    unbindService(donateConnection);
                } catch (IllegalArgumentException e) {
                    Log.d(TAG, "cannot unbind donateConnection", e);
                }
            }
            donateConnection = null;
        }
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        try {
            deleteQrCodeIfNeeded();
        } catch (SecurityException e) { // NOSONAR
            // do nothing
        }
    }

    @Override
    @CallSuper
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.alipay) {
            donateViaAlipay();
        } else if (id == R.id.wechat) {
            donateViaWechat();
        } else if (id == R.id.play) {
            donateViaPlay();
        }
    }

    private synchronized void donateViaPlay() {
        HandlerThread thread = new HandlerThread("DonateService");
        thread.start();
        unbindDonateService();
        donateConnection = new PlayServiceConnection(PlayServiceConnection.MESSAGE_DONATE,
                thread.getLooper(), this);
        Intent serviceIntent = new Intent(PlayServiceConnection.ACTION_BIND);
        serviceIntent.setPackage(PACKAGE_PLAY);
        if (!bindService(serviceIntent, donateConnection, Context.BIND_AUTO_CREATE)) {
            unbindService(donateConnection);
            donateConnection = null;
        }
    }

    private void donateViaAlipay() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getAlipayLink()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startDonateActivity(intent, "alipay");
    }

    private void donateViaWechat() {
        File qrCode = getWechatQrCode();
        if (qrCode != null) {
            if (qrCode.exists()) {
                copyQrCodeAndDonate();
            } else {
                showDonateDialog();
                new WechatTask(this).execute(getWechatLink(), qrCode.getAbsolutePath());
            }
        } else {
            hideWechat();
        }
    }

    void copyQrCodeAndDonate() {
        Uri uri = copyQrCode();
        if (uri != null && !stopped) {
            refreshQrCode(uri);
            new WechatFragment().show(getFragmentManager(), FRAGMENT_DONATION_WECHAT);
        }
    }

    void hideWechat() {
        findViewById(R.id.wechat).setVisibility(View.GONE);
    }

    private boolean mayHasPermission(String permission) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || hasPermission(permission);
    }

    private boolean hasPermission(String permission) {
        return checkPermission(permission, Process.myPid(), Process.myUid()) ==
                PackageManager.PERMISSION_GRANTED;
    }

    @Override
    @CallSuper
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_WECHAT_DONATE_SDA && data != null) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .edit().putString(KEY_WECHAT_DONATE_SDA, uri.toString()).apply();
                donateViaWechat();
            } else {
                hideWechat();
            }
        } else if (requestCode == REQUEST_PLAY_DONATE && data != null) {
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
            if (PlayServiceConnection.verify(getPlayModulus(), purchaseData, dataSignature)) {
                activatePlay();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    @CallSuper
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (PERMISSION_WECHAT_DONATE == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                donateViaWechat();
            } else {
                Toast.makeText(this, R.string.donation_wechat_permission, Toast.LENGTH_LONG).show();
                hideWechat();
            }
        }
    }

    private File getWechatQrCode() {
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir == null) {
            return null;
        }
        return new File(externalFilesDir, "donate_wechat.png");
    }

    private void refreshQrCode(Uri qrCode) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(qrCode);
        sendBroadcast(mediaScanIntent);
    }

    private boolean isFile(Uri uri) {
        return uri != null && "file".equals(uri.getScheme());
    }

    private void deleteQrCodeIfNeeded() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String wechatDonateUri = preferences.getString(KEY_WECHAT_DONATE_URI, null);
        if (wechatDonateUri != null) {
            Uri qrCode = Uri.parse(wechatDonateUri);
            if (isFile(qrCode)) {
                File path = new File(qrCode.getPath());
                if (path.exists() && path.delete()) {
                    refreshQrCode(qrCode);
                }
            } else {
                DocumentFile documentFile = DocumentFile.fromSingleUri(this, qrCode);
                if (documentFile.exists() && documentFile.delete()) {
                    refreshQrCode(qrCode);
                }
            }
            preferences.edit().putString(KEY_WECHAT_DONATE_URI, null).apply();
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
        if (fragment != null && !stopped) {
            fragment.dismiss();
        }
    }

    private void showDonateDialog() {
        if (!stopped) {
            new ProgressFragment().show(getFragmentManager(), FRAGMENT_DONATION_PROGRESS);
        }
    }

    private void activateDonations() {
        Collection<DonateItem> items = new ArrayList<>(0x3);
        if (!TextUtils.isEmpty(getAlipayLink())) {
            checkPackage(items, R.id.alipay, PACKAGE_ALIPAY);
        }
        boolean canSupportWechat = mayHasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (canSupportWechat && !TextUtils.isEmpty(getWechatLink())) {
            checkPackage(items, R.id.wechat, PACKAGE_WECHAT);
        }
        if (items.isEmpty()) {
            mDonationTip.setText(canSupportWechat ? R.string.donation_unsupported_wechat :
                    R.string.donation_unsupported);
            mDonation.setVisibility(mShowDonation ? View.VISIBLE : View.GONE);
        } else {
            mDonationTip.setText(R.string.donation);
            showDonate();
            new DonateTask(this, false).execute(items.toArray(new DonateItem[items.size()]));
        }
    }

    private void checkPackage(Collection<DonateItem> items, int resId, String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            DonateItem item = new DonateItem();
            item.intent = intent;
            item.textView = findViewById(resId);
            item.textView.setOnClickListener(this);
            if (items != null) {
                items.add(item);
            }
        }
    }

    protected abstract String getAlipayLink();

    protected abstract String getWechatLink();

    protected abstract BigInteger getPlayModulus();

    protected boolean acceptDonation() {
        return true;
    }

    protected boolean hasPlay() {
        return getPackageManager().getLaunchIntentForPackage(PACKAGE_PLAY) != null;
    }

    protected String getApplicationId() {
        return getPackageName();
    }

    protected boolean isPlay() {
        return hasPlay() && PACKAGE_PLAY.equals(getPackageManager()
                .getInstallerPackageName(getApplicationId()));
    }

    private Uri getQrCodeUri() {
        String name = getApplicationId() + ".donate.wechat.png";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String cachedUri = preferences.getString(KEY_WECHAT_DONATE_SDA, null);
            DocumentFile file = null;
            if (cachedUri != null) {
                try {
                    file = DocumentFile.fromTreeUri(this, Uri.parse(cachedUri))
                            .createFile("image/png", name);
                } catch (SecurityException e) { // NOSONAR
                    // do nothing
                }
            }
            if (file == null) {
                StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
                StorageVolume sv = sm.getPrimaryStorageVolume();
                Intent intent = sv.createAccessIntent(Environment.DIRECTORY_PICTURES);
                try {
                    startActivityForResult(intent, REQUEST_WECHAT_DONATE_SDA);
                } catch (ActivityNotFoundException e) { // NOSONAR
                    // do nothing
                }
                return Uri.EMPTY;
            }
            return file.getUri();
        } else if (hasPermission(WECHAT_DONATE_PERMISSION)) {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (dir == null) {
                return null;
            }
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            return Uri.fromFile(new File(dir, name));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[] {WECHAT_DONATE_PERMISSION},
                    PERMISSION_WECHAT_DONATE);
            return Uri.EMPTY;
        } else {
            return null;
        }
    }

    private Uri copyQrCode() {
        File qrCode = getWechatQrCode();
        if (qrCode == null || !qrCode.isFile()) {
            hideWechat();
            return null;
        }

        final Uri uri = getQrCodeUri();
        if (uri == null) {
            hideWechat();
            return null;
        } else if (Uri.EMPTY.equals(uri)) {
            return null;
        }

        try (
                OutputStream outputStream = isFile(uri) ? new FileOutputStream(uri.getPath()) :
                        getContentResolver().openOutputStream(uri);
                FileInputStream fis = new FileInputStream(qrCode)
        ) {
            if (outputStream == null) {
                hideWechat();
                return null;
            }
            byte[] bytes = new byte[0x2000];
            int length;
            while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                outputStream.write(bytes, 0, length);
            }
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit().putString(KEY_WECHAT_DONATE_URI, uri.toString()).apply();
            return uri;
        } catch (IOException e) {
            // IOException
            hideWechat();
            return null;
        }
    }

    static BitmapDrawable cropDrawable(Resources resources, BitmapDrawable icon, int width) {
        if (icon.getMinimumWidth() > width) {
            @SuppressWarnings("SuspiciousNameCombination")
            Bitmap bitmap = Bitmap.createScaledBitmap(icon.getBitmap(), width, width, false);
            return new BitmapDrawable(resources, bitmap);
        }
        return icon;
    }

    @CallSuper
    public void showPlay(@Nullable Collection<String> purchased) {
        if (purchased == null) {
            if (isPlay()) {
                mDonationTip.setText(R.string.donation_play_unavailable);
                mDonation.setVisibility(View.GONE);
            }
        } else if (canDonatePlay(purchased)) {
            Collection<DonateItem> items = new ArrayList<>(0x1);
            checkPackage(items, R.id.play, PACKAGE_PLAY);
            if (!items.isEmpty()) {
                mDonationTip.setText(R.string.donation);
                showDonate();
                new DonateTask(this, true).execute(items.toArray(new DonateItem[items.size()]));
            }
        }
    }

    private void showDonate() {
        if (!canDonate) {
            canDonate = true;
            onShowDonate();
        }
    }

    protected void onShowDonate() {

    }

    @CallSuper
    public void showPlayCheck() {
        if (isPlay()) {
            mDonationTip.setText(R.string.donation_play_checking);
            findViewById(R.id.play).setVisibility(View.GONE);
        }
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
            Log.d(TAG, "Can't donate");
        }
        unbindService();
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
        TextView textView;
    }

    static BitmapDrawable bitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return (BitmapDrawable) drawable;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && drawable instanceof AdaptiveIconDrawable) {
            return bitmap(((AdaptiveIconDrawable) drawable).getForeground());
        } else {
            return null;
        }
    }

}
