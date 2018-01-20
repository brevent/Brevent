package me.piebridge.brevent.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.IconDrawableFactory;

import me.piebridge.brevent.BuildConfig;

/**
 * Created by thom on 2017/1/25.
 */
public class AppsIconTask extends AsyncTask<Object, Void, AppsItemViewHolder> {

    @Override
    protected AppsItemViewHolder doInBackground(Object... params) {
        BreventApplication application = (BreventApplication) params[0];
        AppsItemViewHolder holder = (AppsItemViewHolder) params[1];
        if (BuildConfig.APPLICATION_ID.equals(holder.packageName)) {
            holder.icon = null;
            return holder;
        }
        PackageManager packageManager = application.getPackageManager();
        String packageName = holder.packageName;
        try {
            PackageInfo packageInfo = application.getInstantPackageInfo(packageName);
            if (packageInfo != null) {
                holder.icon = IconDrawableFactory.newInstance(application)
                        .getBadgedIcon(packageInfo.applicationInfo);
            } else {
                Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
                if (launchIntent == null) {
                    holder.icon = packageManager.getApplicationIcon(packageName);
                } else {
                    holder.icon = packageManager.resolveActivity(launchIntent, 0).activityInfo
                            .loadIcon(packageManager);
                }
            }
        } catch (PackageManager.NameNotFoundException e) { // NOSONAR
            // do nothing
        }
        return holder;
    }

    @Override
    protected void onPostExecute(AppsItemViewHolder holder) {
        if (BuildConfig.APPLICATION_ID.equals(holder.packageName)) {
            holder.iconView.setImageResource(BuildConfig.ICON);
        } else if (holder.icon != null) {
            holder.iconView.setImageDrawable(holder.icon);
            holder.icon = null;
        }
    }

}
