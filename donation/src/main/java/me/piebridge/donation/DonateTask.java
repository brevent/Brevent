package me.piebridge.donation;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

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
            PackageManager packageManager = context.getPackageManager();
            for (DonateActivity.DonateItem item : params) {
                ActivityInfo ai = packageManager.resolveActivity(item.intent, 0).activityInfo;
                item.label = ai.loadLabel(packageManager);
                item.icon = ai.loadIcon(packageManager);
                this.publishProgress(item);
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(DonateActivity.DonateItem... values) {
        if (mReference.get() != null) {
            DonateActivity.DonateItem item = values[0];
            ImageView donate = item.imageView;
            donate.setContentDescription(item.label);
            donate.setImageDrawable(item.icon);
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