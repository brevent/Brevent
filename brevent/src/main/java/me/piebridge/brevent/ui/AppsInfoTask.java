package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.util.List;

import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/2/4.
 */
public class AppsInfoTask extends AsyncTask<Context, Integer, Void> {

    private final AppsItemAdapter mAdapter;

    AppsInfoTask(AppsItemAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    protected void onPreExecute() {
        if (mAdapter != null && mAdapter.getActivity() != null) {
            mAdapter.getActivity().showAppProgress(R.string.process_retrieving_apps, 0, 0);
        }
    }

    @Override
    protected Void doInBackground(Context... params) {
        Context context = params[0];

        PackageManager packageManager = context.getPackageManager();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showAllApps = sp.getBoolean(SettingsFragment.SHOW_ALL_APPS,
                SettingsFragment.DEFAULT_SHOW_ALL_APPS);

        AppsLabelLoader labelLoader = new AppsLabelLoader(context);
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);
        int max = installedPackages.size();
        int progress = 0;
        int size = 0;
        for (PackageInfo pkgInfo : installedPackages) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            if (appInfo.enabled && mAdapter.accept(packageManager, pkgInfo, showAllApps)) {
                String label = labelLoader.loadLabel(packageManager, pkgInfo);
                mAdapter.addPackage(appInfo.packageName, label);
                ++size;
            }
            publishProgress(++progress, max, size);
        }
        labelLoader.onCompleted();
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (mAdapter != null && mAdapter.getActivity() != null) {
            mAdapter.getActivity().showAppProgress(progress[0], progress[1], progress[2]);
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        if (mAdapter != null && mAdapter.getActivity() != null) {
            mAdapter.getActivity().hideAppProgress();
            mAdapter.onCompleted();
        }
    }

}
