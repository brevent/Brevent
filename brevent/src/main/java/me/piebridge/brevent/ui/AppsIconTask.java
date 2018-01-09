package me.piebridge.brevent.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

/**
 * Created by thom on 2017/1/25.
 */
public class AppsIconTask extends AsyncTask<Object, Void, AppsItemViewHolder> {

    @Override
    protected AppsItemViewHolder doInBackground(Object... params) {
        PackageManager packageManager = (PackageManager) params[0];
        AppsItemViewHolder holder = (AppsItemViewHolder) params[1];
        try {
            Intent launchIntent = packageManager.getLaunchIntentForPackage(holder.packageName);
            if (launchIntent == null) {
                holder.icon = packageManager.getApplicationIcon(holder.packageName);
            } else {
                holder.icon = packageManager.resolveActivity(launchIntent, 0).activityInfo
                        .loadIcon(packageManager);
            }
        } catch (PackageManager.NameNotFoundException e) { // NOSONAR
            // do nothing
        }
        return holder;
    }

    @Override
    protected void onPostExecute(AppsItemViewHolder holder) {
        if (holder.icon != null) {
            holder.iconView.setImageDrawable(holder.icon);
            holder.icon = null;
        }
    }

}
