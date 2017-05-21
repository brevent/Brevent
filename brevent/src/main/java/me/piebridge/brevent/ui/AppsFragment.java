package me.piebridge.brevent.ui;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/1/25.
 */
public abstract class AppsFragment extends Fragment {

    private static final String PACKAGE_FRAMEWORK = "android";

    private View mView;

    private RecyclerView mRecycler;

    private AppsItemAdapter mAdapter;

    private AppsItemHandler mHandler;

    private boolean mResumed;

    private boolean mVisible;

    private boolean mLoaded;

    private volatile boolean mExpired;

    private Signature[] frameworkSignatures;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_brevent, container, false);
            mRecycler = (RecyclerView) mView.findViewById(R.id.recycler);
        }
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mResumed = true;
        lazyLoad();
        if (mLoaded) {
            mHandler.sendEmptyMessage(AppsItemHandler.MSG_UPDATE_ITEM);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        mVisible = isVisibleToUser;
        lazyLoad();
    }

    private void lazyLoad() {
        if (mVisible && mResumed && mRecycler != null) {
            if (mRecycler.getAdapter() == null) {
                BreventActivity activity = (BreventActivity) getActivity();
                if (activity != null && !activity.isStopped()) {
                    activity.showFragmentAsync(this, 0);
                }
            } else if (mExpired) {
                mExpired = false;
                mAdapter.retrievePackagesAsync();
            }
        }
    }

    @Override
    public void onStop() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mAdapter = null;
        mHandler = null;
        mRecycler = null;
        mView = null;
        super.onDestroy();
    }

    public abstract boolean accept(PackageManager packageManager, ApplicationInfo applicationInfo);

    public void refresh() {
        if (mAdapter != null) {
            mAdapter.updateAppsInfo();
        }
    }

    @UiThread
    public void show() {
        if (mHandler == null) {
            mHandler = new AppsItemHandler(this, mRecycler);
        } else {
            mHandler.obtainMessage();
        }
        mRecycler.addOnScrollListener(new AppsScrollListener(mHandler));
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecycler.addItemDecoration(
                new DividerItemDecoration(mRecycler.getContext(), LinearLayoutManager.VERTICAL));
        if (mAdapter == null) {
            mAdapter = new AppsItemAdapter(this, mHandler);
        } else {
            mAdapter.getActivity();
        }
        mRecycler.setAdapter(mAdapter);
        mLoaded = true;
    }

    public void update(Collection<String> packageNames) {
        BreventActivity breventActivity = (BreventActivity) getActivity();
        if (mAdapter != null && breventActivity != null) {
            List<AppsInfo> appsInfoList = mAdapter.getAppsInfo();
            int size = appsInfoList.size();
            for (int i = 0; i < size; ++i) {
                AppsInfo info = appsInfoList.get(i);
                if (info.isPackage() && packageNames.contains(info.packageName)) {
                    mAdapter.notifyItemChanged(i);
                }
            }
            breventActivity.setSelectCount(mAdapter.getSelectedSize());
        }
    }

    public void select(Collection<String> packageNames) {
        update(mAdapter.select(packageNames));
    }

    public void clearSelected() {
        update(mAdapter.clearSelected());
    }

    public int getSelectedSize() {
        if (mAdapter == null) {
            return 0;
        } else {
            return mAdapter.getSelectedSize();
        }
    }

    public void selectInverse() {
        update(mAdapter.selectInverse());
    }

    public void selectAll() {
        update(mAdapter.selectAll());
    }

    public void retrievePackages() {
        mAdapter.retrievePackages();
    }

    public Collection<String> getSelected() {
        return mAdapter.getSelected();
    }

    protected final boolean isSystemPackage(int flags) {
        return (flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    protected final boolean isFrameworkPackage(PackageManager packageManager, String packageName) {
        if (packageManager.checkSignatures(PACKAGE_FRAMEWORK, BuildConfig.APPLICATION_ID) !=
                PackageManager.SIGNATURE_MATCH) {
            return packageManager.checkSignatures(PACKAGE_FRAMEWORK, packageName) ==
                    PackageManager.SIGNATURE_MATCH;
        } else {
            SharedPreferences preferences = null;
            Context context = getActivity();
            if (context != null) {
                preferences = context.getSharedPreferences("signature", Context.MODE_PRIVATE);
                if (preferences.contains(packageName)) {
                    return preferences.getBoolean(packageName, false);
                }
            }
            boolean signature = Arrays.equals(getFrameworkSignatures(packageManager),
                    BreventActivity.getSignatures(packageManager, packageName));
            if (preferences != null) {
                preferences.edit().putBoolean(packageName, signature).apply();
            }
            return signature;
        }
    }

    private Signature[] getFrameworkSignatures(PackageManager packageManager) {
        if (frameworkSignatures == null) {
            synchronized (this) {
                if (frameworkSignatures == null) {
                    frameworkSignatures = BreventActivity.getSignatures(packageManager,
                            PACKAGE_FRAMEWORK);
                }
            }
        }
        return frameworkSignatures;
    }

    public boolean supportAllApps() {
        return false;
    }

    public void setExpired() {
        if (mAdapter != null) {
            mExpired = true;
            lazyLoad();
        }
    }

    public final boolean isImportant(String packageName) {
        return ((BreventActivity) getActivity()).isImportant(packageName);
    }

    public final boolean isGcm(String packageName) {
        return ((BreventActivity) getActivity()).isGcm(packageName);
    }

    public final boolean isFavorite(String packageName) {
        return ((BreventActivity) getActivity()).isFavorite(packageName);
    }

    public final String getLabel(String label, String packageName) {
        return ((BreventActivity) getActivity()).getLabel(label, packageName);
    }

}
