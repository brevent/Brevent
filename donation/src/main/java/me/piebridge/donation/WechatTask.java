package me.piebridge.donation;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.ArrayMap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * wechat QrCode task
 * <p>
 * Created by thom on 2017/2/13.
 */
class WechatTask extends AsyncTask<String, Void, Boolean>
        implements DialogInterface.OnCancelListener {

    private final WeakReference<DonateActivity> mReference;

    WechatTask(DonateActivity activity) {
        mReference = new WeakReference<>(activity);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        DonateActivity donateActivity = mReference.get();
        String link = params[0];
        File path = new File(params[1]);

        PackageManager packageManager = donateActivity.getPackageManager();
        Intent launcher = packageManager.getLaunchIntentForPackage(donateActivity.getPackageName());
        BitmapDrawable drawable = (BitmapDrawable) packageManager.resolveActivity(launcher, 0)
                .activityInfo.loadIcon(packageManager);
        Resources resources = donateActivity.getResources();
        drawable = DonateActivity.cropDrawable(resources, drawable,
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size));
        Bitmap bitmap;
        try {
            bitmap = createCode(link, drawable.getBitmap());
        } catch (WriterException e) {
            // do nothing
            return false;
        }
        try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                FileOutputStream fos = new FileOutputStream(path)
        ) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            fos.write(bos.toByteArray());
            fos.flush();

            bitmap.recycle();
            return true;
        } catch (IOException e) {
            // do nothing
            return false;
        }
    }

    private Bitmap createCode(String content, Bitmap logo) throws WriterException {
        int logoSize = Math.max(logo.getWidth(), logo.getHeight());
        int size = logoSize * 0x5;

        Map<EncodeHintType, Object> hints = new ArrayMap<>(0x2);
        hints.put(EncodeHintType.MARGIN, 0x2);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE,
                size, size, hints);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int left = (width - logo.getWidth()) / 2;
        int top = (height - logo.getHeight()) / 2;
        int leftEnd = left + logo.getWidth();
        int topEnd = top + logo.getHeight();
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if ((x >= left && x < leftEnd) && (y >= top && y < topEnd)) {
                    bitmap.setPixel(x, y, Color.WHITE);
                } else {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.DKGRAY : Color.WHITE);
                }
            }
        }

        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(logo, left, top, null);
        canvas.save();
        canvas.restore();

        return bitmap;
    }

    @Override
    protected void onPostExecute(Boolean param) {
        DonateActivity donateActivity = mReference.get();
        if (donateActivity != null) {
            donateActivity.hideDonateDialog();
            if (!isCancelled() && Boolean.TRUE.equals(param)) {
                donateActivity.copyQrCodeAndDonate();
            } else {
                donateActivity.hideWechat();
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        cancel(false);
    }
}