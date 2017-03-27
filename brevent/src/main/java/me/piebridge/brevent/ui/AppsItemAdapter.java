package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.Process;
import android.support.annotation.ColorInt;
import android.support.v4.util.ArraySet;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.TimeUtils;

/**
 * Created by thom on 2017/1/25.
 */
public class AppsItemAdapter extends RecyclerView.Adapter implements View.OnLongClickListener, View.OnClickListener {

    private static final int VIEW_TYPE_SECTION = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final String[] mStatus;

    private final List<AppsInfo> mAppsInfo;

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

    public AppsItemAdapter(AppsFragment fragment, Handler handler) {
        mFragment = fragment;
        mHandler = handler;
        mAppsInfo = new ArrayList<>();
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section, parent, false);
            AppsSectionViewHolder viewHolder = new AppsSectionViewHolder(view);
            viewHolder.statusView = (TextView) view.findViewById(R.id.status);
            viewHolder.countView = (TextView) view.findViewById(R.id.count);
            return viewHolder;
        } else {
            CardView view = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_apps, parent, false);
            AppsItemViewHolder viewHolder = new AppsItemViewHolder(mFragment, view);
            viewHolder.cardView = view;
            viewHolder.iconView = (ImageView) view.findViewById(R.id.icon);
            viewHolder.nameView = (TextView) view.findViewById(R.id.name);
            viewHolder.syncView = (ImageView) view.findViewById(R.id.sync);
            viewHolder.statusView = (ImageView) view.findViewById(R.id.status);
            viewHolder.descriptionView = (TextView) view.findViewById(R.id.description);
            viewHolder.inactiveView = (TextView) view.findViewById(R.id.inactive);
            view.setOnLongClickListener(this);
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
        BreventActivity breventActivity = getActivity();
        if (breventActivity != null) {
            updateInactive(breventActivity, viewHolder);
            updateStatus(breventActivity, viewHolder);
            updateDescription(breventActivity, viewHolder);
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
                String label = mFragment.getActivity().getString(R.string.important_gcm, viewHolder.label);
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

    private void updateInactive(BreventActivity breventActivity, AppsItemViewHolder viewHolder) {
        int inactive = breventActivity.getInactive(viewHolder.packageName);
        if (viewHolder.inactive != inactive) {
            viewHolder.inactive = inactive;
            if (viewHolder.inactive > 0) {
                viewHolder.inactiveView.setVisibility(View.VISIBLE);
                viewHolder.inactiveView.setText(DateUtils.formatElapsedTime(TimeUtils.now() - viewHolder.inactive));
            } else {
                viewHolder.inactiveView.setVisibility(View.GONE);
            }
        }
    }

    private void updateStatus(BreventActivity breventActivity, AppsItemViewHolder viewHolder) {
        String packageName = viewHolder.packageName;
        int statusIcon = breventActivity.getStatusIcon(packageName);
        if (viewHolder.statusIconRes != statusIcon) {
            viewHolder.statusIconRes = statusIcon;
            if (statusIcon == 0) {
                viewHolder.statusView.setVisibility(View.GONE);
            } else {
                viewHolder.statusView.setVisibility(View.VISIBLE);
                viewHolder.statusView.setImageResource(statusIcon);
            }
        }
        if (breventActivity.isBrevent(packageName) && breventActivity.isPriority(packageName)) {
            viewHolder.syncView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.syncView.setVisibility(View.INVISIBLE);
        }
    }

    private void updateDescription(BreventActivity breventActivity, AppsItemViewHolder viewHolder) {
        String description = breventActivity.getDescription(viewHolder.packageName);
        if (description == null) {
            viewHolder.descriptionView.setText(R.string.process_not_running);
        } else {
            viewHolder.descriptionView.setText(description);
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

    public void onCompleted() {
        mCompleted = true;
        BreventActivity breventActivity = getActivity();
        if (breventActivity != null) {
            updateAppsInfo();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v instanceof CardView) {
            onSelected((CardView) v);
            return true;
        } else {
            return false;
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
        BreventActivity breventActivity = getActivity();
        if (breventActivity != null) {
            updateIcon(holder);
            breventActivity.setSelectCount(mSelected.size());
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
        mAppsInfo.clear();
        mPackages.clear();
        mHandler.sendEmptyMessage(AppsItemHandler.MSG_STOP_UPDATE);
        new AppsInfoTask(this).execute(activity);
    }

    public void updateAppsInfo() {
        if (mCompleted) {
            if (updateAppsStatus()) {
                Collections.sort(mAppsInfo);
                notifyDataSetChanged();
            }
            mHandler.sendEmptyMessage(AppsItemHandler.MSG_UPDATE_ITEM);
        }
    }

    private boolean updateAppsStatus() {
        BreventActivity activity = getActivity();
        if (activity == null) {
            return false;
        }
        boolean changed = false;
        SparseIntArray counter = new SparseIntArray();
        SparseArray<AppsInfo> appsInfoStatus = new SparseArray<>();
        for (AppsInfo appsInfo : mAppsInfo) {
            if (appsInfo.isPackage()) {
                int status = activity.getStatus(appsInfo.packageName);
                if (appsInfo.status != status) {
                    appsInfo.status = status;
                    changed = true;
                }
                int oldValue = counter.get(status, 0);
                counter.put(status, oldValue + 1);
            } else {
                appsInfoStatus.put(appsInfo.status, appsInfo);
            }
        }
        if (!changed) {
            return false;
        }
        mHandler.sendEmptyMessage(AppsItemHandler.MSG_STOP_UPDATE);
        int size = counter.size();
        for (int i = 0; i < size; ++i) {
            int status = counter.keyAt(i);
            String label = String.valueOf(counter.valueAt(i));
            AppsInfo appsInfo = appsInfoStatus.get(status);
            if (appsInfo == null) {
                mAppsInfo.add(new AppsInfo(status, label));
            } else {
                appsInfo.label = label;
                appsInfoStatus.remove(status);
            }
        }
        size = appsInfoStatus.size();
        for (int i = 0; i < size; ++i) {
            mAppsInfo.remove(appsInfoStatus.valueAt(i));
        }
        return true;
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

    public void addPackage(String packageName, String label) {
        mPackages.add(packageName);
        mAppsInfo.add(new AppsInfo(packageName, label));
    }

    public boolean accept(PackageManager pm, ApplicationInfo appInfo, boolean showAllApps) {
        BreventActivity activity = getActivity();
        String packageName = appInfo.packageName;
        if (appInfo.uid < Process.FIRST_APPLICATION_UID) {
            if (activity.isBrevent(packageName)) {
                activity.unbrevent(packageName);
            }
            return false;
        }
        boolean hasLaunchIntent = pm.getLaunchIntentForPackage(packageName) != null;
        if (mFragment.isSystemPackage(appInfo.flags) && !hasLaunchIntent) {
            if (activity.isBrevent(packageName)) {
                activity.unbrevent(packageName);
            }
        }
        return (activity.isLauncher(packageName) || mFragment.supportAllApps() || showAllApps || hasLaunchIntent)
                && mFragment.accept(pm, appInfo);
    }

}
