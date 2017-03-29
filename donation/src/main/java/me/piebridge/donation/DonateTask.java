package me.piebridge.donation;


import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/**
 * Donate Task - show available donations
 * <p>
 * Created by thom on 2017/2/13.
 */
class DonateTask extends AsyncTask<DonateActivity.DonateItem, DonateActivity.DonateItem, Void> {

    private final WeakReference<Context> mReference;

    private final boolean mUnbindService;

    DonateTask(Context context, boolean unbindService) {
        mReference = new WeakReference<>(context);
        mUnbindService = unbindService;
    }

    @Override
    protected Void doInBackground(DonateActivity.DonateItem... params) {
        Context context = mReference.get();
        if (context != null) {
            Resources resources = context.getResources();
            PackageManager packageManager = context.getPackageManager();
            int size = resources.getDimensionPixelSize(android.R.dimen.app_icon_size);
            for (DonateActivity.DonateItem item : params) {
                ActivityInfo activityInfo =
                        packageManager.resolveActivity(item.intent, 0).activityInfo;
                item.label = activityInfo.loadLabel(packageManager);
                item.icon = DonateActivity.cropDrawable(resources,
                        (BitmapDrawable) activityInfo.loadIcon(packageManager), size);
                this.publishProgress(item);
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(DonateActivity.DonateItem... values) {
        if (mReference.get() != null) {
            DonateActivity.DonateItem item = values[0];
            TextView donate = item.textView;
            donate.setText(item.label);
            donate.setContentDescription(item.label);
            donate.setCompoundDrawablesWithIntrinsicBounds(null, item.icon, null, null);
            donate.setClickable(true);
            donate.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPostExecute(Void params) {
        Context context = mReference.get();
        if (context != null) {
            ((DonateActivity) context).showDonation();
            if (mUnbindService) {
                ((DonateActivity) context).unbindService();
            }
        }
    }

}