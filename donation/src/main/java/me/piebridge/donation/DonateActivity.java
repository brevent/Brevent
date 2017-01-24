package me.piebridge.donation;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Donate activity, support alipay, wechat, paypal, google
 * <p>
 * Created by thom on 2017/2/9.
 */
public abstract class DonateActivity extends Activity implements View.OnClickListener {

    private static final String PACKAGE_ALIPAY = "com.eg.android.AlipayGphone";

    static final String PACKAGE_WECHAT = "com.tencent.mm";

    private static final String PACKAGE_PAYPAL = "com.paypal.android.p2pmobile";

    private static final String PACKAGE_VENDING = "com.android.vending";

    private static final int REQUEST_WECHAT_DONATE_SDA = 0x4121;

    private static final String KEY_WECHAT_DONATE_SDA = "donation.wechat.sda";
    private static final String KEY_WECHAT_DONATE_URI = "donation.wechat.uri";

    private static final String WECHAT_DONATE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final String FRAGMENT_DONATION_WECHAT = "fragment_donation_wechat";

    private static final String FRAGMENT_DONATION_PROGRESS = "fragment_donation_progress";

    private View mDonation;
    private TextView mDonationTip;

    @CallSuper
    public void onStart() {
        super.onStart();
        mDonation = findViewById(R.id.donation);
        mDonationTip = (TextView) findViewById(R.id.donation_tip);
        updateDonations();
    }

    public final void updateDonations() {
        if (acceptDonation()) {
            if (isPlay()) {
                activatePlay();
            } else {
                activateDonations();
            }
        } else {
            mDonation.setVisibility(View.GONE);
        }
    }

    private void activatePlay() {
        // TODO
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        hideDonateDialog();
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
        } else if (id == R.id.paypal) {
            donateViaPaypal();
        }
    }

    private void donateViaAlipay() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getAlipayLink()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startDonateActivity(intent);
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
        if (copyQrCode()) {
            new WechatFragment().show(getFragmentManager(), FRAGMENT_DONATION_WECHAT);
        }
    }

    void hideWechat() {
        findViewById(R.id.wechat).setVisibility(View.GONE);
    }

    private void donateViaPaypal() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getPaypalLink()));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(browser, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null) {
            intent.setPackage(resolveInfo.activityInfo.packageName);
        }
        startDonateActivity(intent);
    }

    private boolean hasPermission(String permission) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                || checkPermission(permission, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    @CallSuper
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_WECHAT_DONATE_SDA) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .edit().putString(KEY_WECHAT_DONATE_SDA, uri.toString()).apply();
                donateViaWechat();
            } else {
                hideWechat();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
                DocumentFile documentFile = DocumentFile.fromSingleUri(getApplicationContext(), qrCode);
                if (documentFile.exists() && documentFile.delete()) {
                    refreshQrCode(qrCode);
                }
            }
            preferences.edit().putString(KEY_WECHAT_DONATE_URI, null).apply();
        }
    }

    void startDonateActivity(Intent intent) {
        showDonateDialog();
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            hideDonateDialog();
        }
    }

    void hideDonateDialog() {
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(FRAGMENT_DONATION_PROGRESS);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void showDonateDialog() {
        new ProgressFragment().show(getFragmentManager(), FRAGMENT_DONATION_PROGRESS);
    }

    private void activateDonations() {
        Collection<DonateItem> items = new ArrayList<>(0x3);
        if (!TextUtils.isEmpty(getAlipayLink())) {
            checkPackage(items, R.id.alipay, PACKAGE_ALIPAY);
        }
        if (!TextUtils.isEmpty(getPaypalLink())) {
            checkPackage(items, R.id.paypal, PACKAGE_PAYPAL);
        }
        boolean canSupportWechat = hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (canSupportWechat && !TextUtils.isEmpty(getWechatLink())) {
            checkPackage(items, R.id.wechat, PACKAGE_WECHAT);
        }
        if (items.isEmpty()) {
            mDonationTip.setText(canSupportWechat ? R.string.donation_unsupported_wechat : R.string.donation_unsupported);
            mDonation.setVisibility(View.VISIBLE);
        } else {
            mDonationTip.setText(R.string.donation);
            Donation donation = new Donation();
            donation.donation = mDonation;
            donation.items = items;
            new DonateTask(this, donation).execute();
        }
    }

    private void checkPackage(Collection<DonateItem> items, int resId, String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            DonateItem item = new DonateItem();
            item.intent = intent;
            item.textView = (TextView) findViewById(resId);
            item.textView.setOnClickListener(this);
            items.add(item);
        }
    }

    protected abstract String getAlipayLink();

    protected abstract String getPaypalLink();

    protected abstract String getWechatLink();

    protected boolean acceptDonation() {
        return true;
    }

    protected boolean isPlay() {
        PackageManager packageManager = getPackageManager();
        return packageManager.getLaunchIntentForPackage(PACKAGE_VENDING) != null
                && PACKAGE_VENDING.equals(packageManager.getInstallerPackageName(getPackageName()));
    }

    private Uri getQrCodeUri() {
        String name = getPackageName() + ".donate.wechat.png";
        if (hasPermission(WECHAT_DONATE_PERMISSION)) {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (dir == null) {
                return null;
            }
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            return Uri.fromFile(new File(dir, name));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String cachedUri = preferences.getString(KEY_WECHAT_DONATE_SDA, null);
            DocumentFile file = null;
            if (cachedUri != null) {
                try {
                    file = DocumentFile.fromTreeUri(this, Uri.parse(cachedUri)).createFile("image/png", name);
                } catch (SecurityException e) { // NOSONAR
                    // do nothing
                }
            }
            if (file == null) {
                StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
                StorageVolume storageVolume = storageManager.getPrimaryStorageVolume();
                Intent intent = storageVolume.createAccessIntent(Environment.DIRECTORY_PICTURES);
                startActivityForResult(intent, REQUEST_WECHAT_DONATE_SDA);
                return Uri.EMPTY;
            }
            return file.getUri();
        } else {
            return null;
        }
    }

    private boolean copyQrCode() {
        File qrCode = getWechatQrCode();
        if (qrCode == null || !qrCode.isFile()) {
            hideWechat();
            return false;
        }

        Uri uri = getQrCodeUri();
        if (uri == null) {
            hideWechat();
            return false;
        } else if (Uri.EMPTY.equals(uri)) {
            return false;
        }

        try (
                OutputStream outputStream = isFile(uri) ? new FileOutputStream(uri.getPath()) : getContentResolver().openOutputStream(uri);
                FileInputStream fis = new FileInputStream(qrCode)
        ) {
            if (outputStream == null) {
                hideWechat();
                return false;
            }
            byte[] bytes = new byte[0x2000];
            int length;
            while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                outputStream.write(bytes, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit().putString(KEY_WECHAT_DONATE_URI, uri.toString()).apply();
            refreshQrCode(uri);
            return true;
        } catch (IOException e) {
            // IOException
            hideWechat();
            return false;
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

    static class DonateItem {
        Intent intent;
        Drawable icon;
        CharSequence label;
        TextView textView;
    }

    static class Donation {
        View donation;
        Collection<DonateItem> items;
    }

}
