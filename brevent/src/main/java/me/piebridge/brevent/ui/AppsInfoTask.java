package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.util.List;

/**
 * Created by thom on 2017/2/4.
 */
public class AppsInfoTask extends AsyncTask<Void, Integer, Boolean> {

    private final AppsItemAdapter mAdapter;

    AppsInfoTask(AppsItemAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    protected void onPreExecute() {
        BreventActivity activity = mAdapter.getActivity();
        if (activity != null) {
            activity.showAppProgress();
        }
    }

    @Override
    protected Boolean doInBackground(Void... args) {
        Context context = mAdapter.getActivity();
        if (context == null) {
            return false;
        }

        PackageManager packageManager = context.getPackageManager();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showAllApps = sp.getBoolean(SettingsFragment.SHOW_ALL_APPS,
                SettingsFragment.DEFAULT_SHOW_ALL_APPS);

        AppsLabelLoader labelLoader = new AppsLabelLoader(context);
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);
        int max = installedPackages.size();
        int progress = 0;
        int size = 0;
        for (PackageInfo packageInfo : installedPackages) {
            BreventActivity activity = mAdapter.getActivity();
            if (activity == null || activity.isStopped()) {
                return false;
            }
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            if (appInfo.enabled && mAdapter.accept(packageManager, packageInfo, showAllApps)) {
                String label = labelLoader.loadLabel(packageManager, packageInfo);
                mAdapter.addPackage(appInfo.packageName, label);
                size++;
            }
            publishProgress(++progress, max, size);
        }
        labelLoader.onCompleted();
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        BreventActivity activity = mAdapter.getActivity();
        if (activity != null) {
            activity.updateAppProgress(progress[0], progress[1], progress[2]);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mAdapter.onCompleted(result == null ? false : result);
    }

}
