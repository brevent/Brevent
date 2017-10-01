package me.piebridge.brevent.ui;

import android.app.usage.UsageStats;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.Process;
import android.support.annotation.ColorInt;
import android.support.v4.util.ArraySet;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventResponse;

/**
 * Created by thom on 2017/1/25.
 */
public class AppsItemAdapter extends RecyclerView.Adapter implements View.OnClickListener {

    private static final int VIEW_TYPE_SECTION = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final String[] mStatus;

    private final List<AppsInfo> mAppsInfo;

    private final List<AppsInfo> mNext;

    private boolean mChanged;

    private final Set<String> mPackages;

    private final Set<String> mSelected;

    private ColorStateList textColorPrimary;
    @ColorInt
    private int cardColorBackgroundDefault = Color.TRANSPARENT;
    @ColorInt
    private int cardColorBackgroundHighlight;

    private Handler mHandler;

    private AppsFragment mFragment;

    private boolean mCompleted;

    private boolean mSuccess;

    public AppsItemAdapter(AppsFragment fragment, Handler handler) {
        mFragment = fragment;
        mHandler = handler;
        mAppsInfo = new ArrayList<>();
        mNext = new ArrayList<>();
        mPackages = new ArraySet<>();
        mStatus = fragment.getResources().getStringArray(R.array.process_status);
        mSelected = new ArraySet<>();
        mHandler.sendEmptyMessage(AppsItemHandler.MSG_RETRIEVE_PACKAGES);

        textColorPrimary = ColorStateList.valueOf(getActivity().mTextColorPrimary);
        cardColorBackgroundHighlight = getActivity().mColorControlHighlight;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SECTION) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_section, parent, false);
            AppsSectionViewHolder viewHolder = new AppsSectionViewHolder(view);
            viewHolder.statusView = view.findViewById(R.id.status);
            viewHolder.countView = view.findViewById(R.id.count);
            return viewHolder;
        } else {
            CardView view = (CardView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_apps, parent, false);
            AppsItemViewHolder viewHolder = new AppsItemViewHolder(mFragment, view);
            viewHolder.cardView = view;
            viewHolder.iconView = view.findViewById(R.id.icon);
            viewHolder.nameView = view.findViewById(R.id.name);
            viewHolder.syncView = view.findViewById(R.id.sync);
            viewHolder.statusView = view.findViewById(R.id.status);
            viewHolder.descriptionView = view.findViewById(R.id.description);
            viewHolder.inactiveView = view.findViewById(R.id.inactive);
            view.setOnClickListener(this);
            viewHolder.iconView.setOnClickListener(this);
            if (cardColorBackgroundDefault == Color.TRANSPARENT) {
                cardColorBackgroundDefault = view.getCardBackgroundColor().getDefaultColor();
            }
            return viewHolder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        AppsInfo appsInfo = mAppsInfo.get(position);
        if (holder instanceof AppsItemViewHolder) {
            AppsItemViewHolder viewHolder = (AppsItemViewHolder) holder;
            if (!appsInfo.packageName.equals(viewHolder.packageName)) {
                viewHolder.packageName = appsInfo.packageName;
                viewHolder.label = appsInfo.label;
                viewHolder.cardView.setTag(viewHolder);
            }
            updateViewHolder(viewHolder);
        } else if (holder instanceof AppsSectionViewHolder) {
            AppsSectionViewHolder viewHolder = (AppsSectionViewHolder) holder;
            viewHolder.statusView.setText(mStatus[appsInfo.status]);
            viewHolder.countView.setText(appsInfo.label);
        }
    }

    private void updateViewHolder(AppsItemViewHolder viewHolder) {
        updateIcon(viewHolder);
        BreventActivity activity = getActivity();
        if (activity != null) {
            updateViewHolder(activity, viewHolder);
        }
    }

    private void updateIcon(AppsItemViewHolder viewHolder) {
        if (mSelected.contains(viewHolder.packageName)) {
            if (mFragment.isImportant(viewHolder.packageName)) {
                String label = mFragment.getLabel(viewHolder.label, viewHolder.packageName);
                viewHolder.nameView.setText(label);
                viewHolder.iconView.setImageResource(R.drawable.ic_error_black_44dp);
            } else if (mFragment.isFavorite(viewHolder.packageName)) {
                String label = mFragment.getLabel(viewHolder.label, viewHolder.packageName);
                viewHolder.nameView.setText(label);
                viewHolder.iconView.setImageResource(R.drawable.ic_favorite_black_44dp);
            } else if (mFragment.isGcm(viewHolder.packageName)) {
                String label = mFragment.getActivity().getString(R.string.important_gcm,
                        viewHolder.label);
                viewHolder.nameView.setText(label);
                viewHolder.iconView.setImageResource(R.drawable.ic_cloud_circle_black_44dp);
            } else {
                viewHolder.nameView.setText(viewHolder.label);
                viewHolder.iconView.setImageResource(R.drawable.ic_check_circle_black_44dp);
            }
            viewHolder.iconView.setImageTintList(textColorPrimary);
            viewHolder.cardView.setBackgroundColor(cardColorBackgroundHighlight);
        } else {
            viewHolder.nameView.setText(viewHolder.label);
            viewHolder.iconView.setImageTintList(null);
            new AppsIconTask().execute(getActivity().getPackageManager(), viewHolder);
            viewHolder.cardView.setBackgroundColor(cardColorBackgroundDefault);
        }
    }

    static void updateViewHolder(BreventActivity activity, AppsItemViewHolder viewHolder) {
        updateInactive(activity, viewHolder);
        updateStatus(activity, viewHolder);
        updateDescription(activity, viewHolder);
    }

    static void updateInactive(BreventActivity activity, AppsItemViewHolder viewHolder) {
        int inactive = activity.getInactive(viewHolder.packageName);
        if (viewHolder.inactive != inactive) {
            viewHolder.inactive = inactive;
            if (viewHolder.inactive > 0) {
                viewHolder.inactiveView.setVisibility(View.VISIBLE);
                int elapsed = BreventResponse.now() - viewHolder.inactive;
                viewHolder.inactiveView.setText(DateUtils.formatElapsedTime(elapsed));
            } else {
                viewHolder.inactiveView.setVisibility(View.GONE);
            }
        }
    }

    static void updateStatus(BreventActivity activity, AppsItemViewHolder viewHolder) {
        String packageName = viewHolder.packageName;
        int statusIcon = activity.getStatusIcon(packageName);
        if (viewHolder.statusIconRes != statusIcon) {
            viewHolder.statusIconRes = statusIcon;
            if (statusIcon == 0) {
                viewHolder.statusView.setVisibility(View.GONE);
            } else {
                viewHolder.statusView.setVisibility(View.VISIBLE);
                viewHolder.statusView.setImageResource(statusIcon);
            }
        }
        if (activity.isBrevent(packageName) && activity.isPriority(packageName)) {
            if (activity.isGcm(packageName)) {
                viewHolder.syncView.setImageResource(R.drawable.ic_cloud_queue_black_24dp);
            } else {
                viewHolder.syncView.setImageResource(R.drawable.ic_sync_black_24dp);
            }
            viewHolder.syncView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.syncView.setVisibility(View.GONE);
        }
    }

    static void updateDescription(BreventActivity activity, AppsItemViewHolder viewHolder) {
        String description = activity.getDescription(viewHolder.packageName);
        if (description != null) {
            viewHolder.descriptionView.setText(description);
        } else if (activity.hasStats()) {
            UsageStats stats = activity.getStats(viewHolder.packageName);
            if (stats == null) {
                viewHolder.descriptionView.setText(R.string.process_no_stats);
            } else {
                viewHolder.descriptionView.setText(activity.getString(R.string.process_stats,
                        DateUtils.formatSameDayTime(stats.getLastTimeUsed(),
                                System.currentTimeMillis(), DateFormat.SHORT, DateFormat.SHORT),
                        DateUtils.formatElapsedTime(stats.getTotalTimeInForeground() / 1000)));
            }
        } else {
            viewHolder.descriptionView.setText(R.string.process_not_running);
        }
    }

    @Override
    public int getItemCount() {
        return mAppsInfo.size();
    }

    @Override
    public int getItemViewType(int position) {
        AppsInfo appsInfo = mAppsInfo.get(position);
        if (appsInfo.isPackage()) {
            return VIEW_TYPE_ITEM;
        } else {
            return VIEW_TYPE_SECTION;
        }
    }

    public List<AppsInfo> getAppsInfo() {
        return mAppsInfo;
    }

    public Context getContext() {
        return getActivity();
    }

    public BreventActivity getActivity() {
        return (BreventActivity) mFragment.getActivity();
    }

    public void onCompleted(boolean success) {
        mCompleted = true;
        mSuccess = success;
        BreventActivity activity = getActivity();
        if (activity != null) {
            if (success) {
                updateAppsInfo();
            }
            activity.hideAppProgress();
        }
    }

    @Override
    public void onClick(View v) {
        if (v instanceof CardView) {
            v.showContextMenu();
        } else if (v instanceof ImageView) {
            if (v.getId() == R.id.icon) {
                onSelected((CardView) v.getParent().getParent());
            }
        }
    }

    private boolean onSelected(CardView v) {
        AppsItemViewHolder holder = (AppsItemViewHolder) v.getTag();
        String packageName = holder.packageName;
        if (packageName == null || !mPackages.contains(packageName)) {
            return false;
        }
        if (mSelected.contains(packageName)) {
            mSelected.remove(packageName);
        } else {
            mSelected.add(packageName);
        }
        BreventActivity activity = getActivity();
        if (activity != null) {
            updateIcon(holder);
            activity.setSelectCount(mSelected.size());
        }
        return true;
    }

    public void retrievePackagesAsync() {
        mHandler.sendEmptyMessage(AppsItemHandler.MSG_RETRIEVE_PACKAGES);
    }

    public void retrievePackages() {
        BreventActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        mCompleted = false;
        mSuccess = false;
        mNext.clear();
        mChanged = true;
        mPackages.clear();
        mHandler.sendEmptyMessage(AppsItemHandler.MSG_STOP_UPDATE);
        new AppsInfoTask(this).execute();
    }

    public void updateAppsInfo() {
        if (mCompleted) {
            if (mSuccess) {
                updateAppsStatus();
                mHandler.sendEmptyMessage(AppsItemHandler.MSG_UPDATE_ITEM);
            } else {
                retrievePackages();
            }
        }
    }

    private boolean updateAppsStatus() {
        BreventActivity activity = getActivity();
        if (activity == null) {
            return false;
        }
        boolean changed = mChanged;
        SparseIntArray counter = new SparseIntArray();
        for (AppsInfo appsInfo : mNext) {
            if (appsInfo.isPackage()) {
                int status = activity.getStatus(appsInfo.packageName);
                if (appsInfo.status != status) {
                    appsInfo.status = status;
                    appsInfo.updated = true;
                    if (!changed) {
                        changed = true;
                    }
                }
                int oldValue = counter.get(status, 0);
                counter.put(status, oldValue + 1);
            }
        }
        if (!changed) {
            return false;
        }
        mChanged = false;
        mHandler.sendEmptyMessage(AppsItemHandler.MSG_STOP_UPDATE);
        Iterator<AppsInfo> it = mNext.iterator();
        while (it.hasNext()) {
            if (!it.next().isPackage()) {
                it.remove();
            }
        }
        int size = counter.size();
        for (int i = 0; i < size; ++i) {
            mNext.add(new AppsInfo(counter.keyAt(i), String.valueOf(counter.valueAt(i))));
        }
        Collections.sort(mNext, getSortMethod());
        if (mAppsInfo.isEmpty()) {
            mAppsInfo.addAll(mNext);
            notifyItemRangeInserted(0, mNext.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffCallback(mAppsInfo, mNext));
            mAppsInfo.clear();
            mAppsInfo.addAll(mNext);
            result.dispatchUpdatesTo(this);
        }
        return true;
    }

    private Comparator<? super AppsInfo> getSortMethod() {
        int checked = SortFragment.getChecked(getActivity());
        switch (checked) {
            case 1:
                return new AppsInfo.SortByUpdateTime();
            case 2:
                return new AppsInfo.SortByLastTime();
            case 3:
                return new AppsInfo.SortByUsageTime();
            case 0:
            default:
                return new AppsInfo.SortByName();
        }
    }

    public Collection<String> getSelected() {
        return new ArraySet<>(mSelected);
    }

    public Collection<String> select(Collection<String> packageNames) {
        Collection<String> changed = new ArraySet<>(mSelected);
        mSelected.clear();
        mSelected.addAll(packageNames);
        mSelected.retainAll(mPackages);
        changed.addAll(mSelected);
        return changed;
    }

    public Collection<String> clearSelected() {
        Collection<String> changed = new ArraySet<>(mSelected);
        mSelected.clear();
        return changed;
    }

    public Collection<String> selectInverse() {
        Set<String> inverse = new ArraySet<>(mPackages);
        inverse.removeAll(mSelected);
        mSelected.clear();
        mSelected.addAll(inverse);
        return mPackages;
    }

    public Collection<String> selectAll() {
        mSelected.addAll(mPackages);
        return mPackages;
    }

    public int getSelectedSize() {
        return mSelected.size();
    }

    public void addPackage(BreventActivity activity, PackageInfo packageInfo, String label) {
        if (mPackages.add(packageInfo.packageName)) {
            AppsInfo appsInfo = new AppsInfo(packageInfo.packageName, label);
            appsInfo.lastUpdateTime = packageInfo.lastUpdateTime;
            appsInfo.stats = activity.getStats(packageInfo.packageName);
            mNext.add(appsInfo);
        }
    }

    public boolean accept(PackageManager pm, PackageInfo packageInfo, boolean showAllApps) {
        BreventActivity activity = getActivity();
        ApplicationInfo appInfo = packageInfo.applicationInfo;
        String packageName = packageInfo.packageName;
        // hard limit
        if (appInfo.uid < Process.FIRST_APPLICATION_UID) {
            return false;
        }
        // filter for fragment
        if (!mFragment.accept(pm, packageInfo)) {
            return false;
        }
        if (activity != null) {
            if (activity.isOverlay(packageName)) {
                // always hide overlay
                return false;
            }
            if (activity.isLauncher(packageName)) {
                // always show launcher
                return true;
            }
            if (activity.isGms(packageName)) {
                // always show gms
                return true;
            }
            if (activity.isBrevent(packageName)) {
                // always for brevented apps
                return true;
            }
        }
        if (showAllApps || mFragment.supportAllApps()) {
            // always for all apps
            return true;
        }
        if (Log.isLoggable(UILog.TAG, Log.VERBOSE)) {
            UILog.v("checking launcher for " + packageName);
        }
        return pm.getLaunchIntentForPackage(packageName) != null;
    }

    private static class DiffCallback extends DiffUtil.Callback {

        private final List<AppsInfo> mOldList;

        private final List<AppsInfo> mNewList;

        DiffCallback(List<AppsInfo> oldList, List<AppsInfo> newList) {
            mOldList = oldList;
            mNewList = newList;
        }

        @Override
        public int getOldListSize() {
            return mOldList.size();
        }

        @Override
        public int getNewListSize() {
            return mNewList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            AppsInfo oldItem = mOldList.get(oldItemPosition);
            AppsInfo newItem = mNewList.get(newItemPosition);
            if (oldItem.isPackage() && newItem.isPackage()) {
                return Objects.equals(oldItem.packageName, newItem.packageName);
            } else if (!oldItem.isPackage() && !newItem.isPackage()) {
                return oldItem.status == newItem.status;
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            AppsInfo oldItem = mOldList.get(oldItemPosition);
            AppsInfo newItem = mNewList.get(newItemPosition);
            if (!oldItem.equals(newItem)) {
                return false;
            } else if (newItem.updated) {
                newItem.updated = false;
                return false;
            } else {
                return true;
            }
        }

    }

}
