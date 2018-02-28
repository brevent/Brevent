package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.protocol.BreventPackageInfo;

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
        BreventPackageInfo packageInfo = application.getInstantPackageInfo(holder.packageName);
        if (packageInfo != null) {
            holder.icon = packageInfo.loadIcon(application);
        } else {
            try {
                holder.icon = loadIcon(application, application.getPackageManager()
                        .getPackageInfo(holder.packageName, 0));
            } catch (PackageManager.NameNotFoundException ignore) {
                // do nothing
            }
        }
        return holder;
    }

    static Drawable loadIcon(Context context, PackageInfo packageInfo) {
        String packageName = packageInfo.packageName;
        PackageManager packageManager = context.getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            return packageManager.resolveActivity(launchIntent, 0)
                    .activityInfo.loadIcon(packageManager);
        } else {
            return packageInfo.applicationInfo.loadIcon(packageManager);
        }
    }

    @Override
    protected void onPostExecute(AppsItemViewHolder holder) {
        if (BuildConfig.APPLICATION_ID.equals(holder.packageName)) {
            holder.iconView.setImageResource(BuildConfig.ICON);
        } else if (holder.icon != null) {
            holder.iconView.setImageDrawable(holder.icon);
            holder.icon = null;
        }
        AppsItemAdapter.updateIcon(holder.iconView, holder.enabled);
    }

}
