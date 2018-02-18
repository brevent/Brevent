package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
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
        PackageInfo packageInfo = application.getInstantPackageInfo(holder.packageName);
        if (packageInfo == null) {
            try {
                PackageManager packageManager = application.getPackageManager();
                packageInfo = packageManager.getPackageInfo(holder.packageName, 0);
            } catch (PackageManager.NameNotFoundException ignore) {
                // do nothing
            }
        }
        if (packageInfo != null) {
            holder.icon = loadIcon(application, packageInfo);
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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return IconDrawableFactory.newInstance(context)
                    .getBadgedIcon(packageInfo.applicationInfo);
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
