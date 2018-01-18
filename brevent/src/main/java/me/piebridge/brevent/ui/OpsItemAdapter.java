package me.piebridge.brevent.ui;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.v4.util.ArraySet;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.app.IAppOpsService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import me.piebridge.brevent.R;
import me.piebridge.brevent.override.HideApiOverride;

/**
 * Created by thom on 2017/1/25.
 */
public class OpsItemAdapter extends RecyclerView.Adapter implements View.OnClickListener {

    private static final SparseArray<String> NAMES = new SparseArray<>();
    private static final SparseArray<String> PERMISSIONS = new SparseArray<>();

    static {
        for (int i = 0; i < HideApiOverride._NUM_OP; ++i) {
            try {
                int op = HideApiOverride.opToSwitch(i);
                if (NAMES.indexOfKey(op) < 0) {
                    NAMES.put(op, HideApiOverride.opToName(op));
                    PERMISSIONS.put(op, HideApiOverride.opToPermission(op));
                }
            } catch (RuntimeException e) {
                UILog.w("Can't add op " + i, e);
                break;
            }
        }
    }

    private static final int VIEW_TYPE_ITEM = 1;

    private static final int VIEW_TYPE_TIME = 2;

    private final OpsFragment mFragment;

    private final String mPackageName;

    private final List<OpsInfo> mOpsInfo;

    private int cardColorBackgroundDefault = Color.TRANSPARENT;

    private int cardColorBackgroundHighlight = Color.TRANSPARENT;

    private Set<Integer> mSelected = new ArraySet<>();

    OpsItemAdapter(OpsFragment fragment, String packageName) {
        mFragment = fragment;
        mPackageName = packageName;
        mOpsInfo = new ArrayList<>();
        SparseArray<OpsInfo> opss = load(getApplication(), packageName);
        int size = NAMES.size();
        for (int i = 0; i < size; ++i) {
            int op = NAMES.keyAt(i);
            OpsInfo opsInfo = getOpsInfo(opss, op);
            opsInfo.name = NAMES.valueAt(i);
            opsInfo.permission = PERMISSIONS.get(op);
            mOpsInfo.add(opsInfo);
        }
        for (OpsInfo opsInfo : mOpsInfo) {
            loadLabel(opsInfo);
        }
        Collections.sort(mOpsInfo, getSortMethod());
    }

    private BreventApplication getApplication() {
        return (BreventApplication) mFragment.getActivity().getApplication();
    }

    private OpsInfo getOpsInfo(SparseArray<OpsInfo> opss, int op) {
        OpsInfo opsInfo = opss.get(op, null);
        if (opsInfo == null) {
            opsInfo = new OpsInfo();
            opsInfo.op = op;
            opsInfo.mode = AppOpsManager.MODE_DEFAULT;
        }
        return opsInfo;
    }

    void refresh() {
        List<OpsInfo> mOld = new ArrayList<>(mOpsInfo);
        SparseArray<OpsInfo> opss = load(getApplication(), mPackageName);
        for (OpsInfo opsInfo : mOpsInfo) {
            opsInfo.update(getOpsInfo(opss, opsInfo.op));
        }
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffCallback(mOld, mOpsInfo));
        result.dispatchUpdatesTo(this);
    }

    void sort() {
        List<OpsInfo> mOld = new ArrayList<>(mOpsInfo);
        Collections.sort(mOpsInfo, getSortMethod());
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffCallback(mOld, mOpsInfo));
        result.dispatchUpdatesTo(this);
    }

    private Comparator<? super OpsInfo> getSortMethod() {
        int checked = OpsSortFragment.getChecked(getActivity());
        switch (checked) {
            case 1:
                return new OpsInfo.SortByName();
            case 2:
                return new OpsInfo.SortByGroup();
            case 3:
                return new OpsInfo.SortByOp();
            case 4:
                return new OpsInfo.SortByMode();
            case 0:
            default:
                return new OpsInfo.SortByTime();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM || viewType == VIEW_TYPE_TIME) {
            CardView view = (CardView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ops, parent, false);
            OpsItemViewHolder viewHolder = new OpsItemViewHolder(mFragment, view);
            viewHolder.cardView = view;
            viewHolder.iconView = view.findViewById(R.id.icon);
            viewHolder.nameView = view.findViewById(R.id.name);
            viewHolder.labelView = view.findViewById(R.id.label);
            viewHolder.timeView = view.findViewById(R.id.time);
            viewHolder.modeView = view.findViewById(R.id.mode);
            if (cardColorBackgroundDefault == Color.TRANSPARENT) {
                cardColorBackgroundDefault = view.getCardBackgroundColor().getDefaultColor();
                cardColorBackgroundHighlight = ColorUtils.resolveColor(getActivity(),
                        android.R.attr.colorControlHighlight);
            }
            view.setOnClickListener(this);
            return viewHolder;
        } else {
            return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        OpsInfo opsInfo = mOpsInfo.get(position);
        OpsItemViewHolder viewHolder = (OpsItemViewHolder) holder;
        if (!opsInfo.name.equals(viewHolder.name)) {
            viewHolder.op = opsInfo.op;
            viewHolder.name = opsInfo.name;
            viewHolder.cardView.setTag(viewHolder);
        }
        updateViewHolder(viewHolder, opsInfo);
    }

    private void loadLabel(OpsInfo opsInfo) {
        if (opsInfo.icon != 0) {
            return;
        }
        Resources resources = getActivity().getResources();
        String packageName = getActivity().getPackageName();
        String id = "op_" + opsInfo.name.toLowerCase(Locale.US);
        int labelRes = resources.getIdentifier(id, "string", packageName);
        int iconRes = resources.getIdentifier(id, "drawable", packageName);
        if (labelRes != 0) {
            opsInfo.label = resources.getString(labelRes);
        }
        if (iconRes != 0) {
            opsInfo.icon = iconRes;
        }
        if (labelRes != 0 && iconRes != 0) {
            return;
        }
        String permission = opsInfo.permission;
        PackageManager packageManager = getActivity().getPackageManager();
        PermissionInfo permissionInfo = getPermissionInfo(packageManager, permission);
        if (permissionInfo != null) {
            if (TextUtils.isEmpty(opsInfo.label)) {
                CharSequence label = permissionInfo.loadLabel(packageManager);
                if (label != null && !Objects.equals(label.toString(), permission)) {
                    opsInfo.label = label.toString();
                }
            }
            opsInfo.icon = permissionInfo.icon;
            String group = permissionInfo.group;
            PermissionGroupInfo groupInfo = getPermissionGroupInfo(packageManager, group);
            if (groupInfo != null) {
                if (opsInfo.icon == 0) {
                    opsInfo.icon = groupInfo.icon;
                }
                CharSequence label = groupInfo.loadLabel(packageManager);
                if (label != null && !Objects.equals(label.toString(), permission)) {
                    opsInfo.groupLabel = label.toString();
                }
            }
        }
        if (opsInfo.icon == 0) {
            opsInfo.icon = R.drawable.ic_perm_device_information_black_24dp;
        }
    }

    private PermissionInfo getPermissionInfo(PackageManager packageManager, String permission) {
        if (TextUtils.isEmpty(permission)) {
            return null;
        }
        try {
            return packageManager.getPermissionInfo(permission, 0);
        } catch (PackageManager.NameNotFoundException e) {
            UILog.w("Can't find permission " + permission, e);
            return null;
        }
    }

    private PermissionGroupInfo getPermissionGroupInfo(PackageManager packageManager, String group) {
        if (TextUtils.isEmpty(group)) {
            return null;
        }
        try {
            return packageManager.getPermissionGroupInfo(group, 0);
        } catch (PackageManager.NameNotFoundException e) {
            UILog.w("Can't find permission group " + group, e);
            return null;
        }
    }

    private void updateViewHolder(OpsItemViewHolder viewHolder, OpsInfo opsInfo) {
        viewHolder.nameView.setText(opsInfo.name.toLowerCase(Locale.US));
        viewHolder.labelView.setText(opsInfo.label);
        viewHolder.modeView.setText(getMode(opsInfo.mode));
        if (opsInfo.time > 0) {
            int res = opsInfo.allow ? R.string.ops_time_allow : R.string.ops_time_reject;
            String text = getActivity().getResources().getString(res,
                    DateUtils.getRelativeTimeSpanString(opsInfo.time, System.currentTimeMillis(),
                            0, DateUtils.FORMAT_ABBREV_ALL));
            viewHolder.timeView.setText(text);
        } else {
            viewHolder.timeView.setText(null);
        }
        viewHolder.iconView.setImageResource(opsInfo.icon);
        updateBackground(viewHolder);
    }

    void updateBackground(OpsItemViewHolder viewHolder) {
        if (mSelected.contains(viewHolder.op)) {
            viewHolder.cardView.setBackgroundColor(cardColorBackgroundHighlight);
        } else {
            viewHolder.cardView.setBackgroundColor(cardColorBackgroundDefault);
        }
    }

    private String getMode(int mode) {
        Resources resources = getActivity().getResources();
        String[] modes = resources.getStringArray(R.array.ops_modes);
        if (mode < modes.length) {
            return modes[mode];
        } else {
            return resources.getString(R.string.ops_mode_unknown);
        }
    }

    private BreventOps getActivity() {
        return (BreventOps) mFragment.getActivity();
    }

    @Override
    public int getItemCount() {
        return mOpsInfo.size();
    }

    @Override
    public int getItemViewType(int position) {
        OpsInfo opsInfo = mOpsInfo.get(position);
        if (opsInfo.time > 0) {
            return VIEW_TYPE_TIME;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public void onClick(View v) {
        if (v instanceof CardView) {
            onSelected((CardView) v);
        }
    }

    private boolean onSelected(CardView v) {
        OpsItemViewHolder holder = (OpsItemViewHolder) v.getTag();
        int op = holder.op;
        if (mSelected.contains(op)) {
            mSelected.remove(op);
        } else {
            mSelected.add(op);
        }
        getActivity().updateSelected(mSelected.size());
        updateBackground(holder);
        return true;
    }

    static List getOpsForPackage(BreventApplication application, String packageName) {
        PackageInfo packageInfo = application.getInstantPackageInfo(packageName);
        int packageUid;
        if (packageInfo != null) {
            packageUid = packageInfo.applicationInfo.uid;
        } else {
            packageUid = getPackageUid(packageName, BreventApplication.getOwner());
        }
        try {
            IBinder service = ServiceManager.getService(Context.APP_OPS_SERVICE);
            IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(service);
            return appOpsService.getOpsForPackage(packageUid, packageName, null);
        } catch (RemoteException | RuntimeException e) {
            UILog.w("Can't getOpsForPackage", e);
            return null;
        }
    }

    private static SparseArray<OpsInfo> load(BreventApplication application, String packageName) {
        SparseArray<OpsInfo> opss = new SparseArray<>();
        for (Object packageOps : getOpsForPackage(application, packageName)) {
            for (Object opEntry : HideApiOverride.getPackageOpsOps(packageOps)) {
                int op = HideApiOverride.getOpEntryOp(opEntry);
                int mode = HideApiOverride.getOpEntryMode(opEntry);
                long time = HideApiOverride.getOpEntryTime(opEntry);
                long rejectTime = HideApiOverride.getOpEntryRejectTime(opEntry);
                int opSwitch = HideApiOverride.opToSwitch(op);
                OpsInfo opsInfo = opss.get(opSwitch, null);
                if (opsInfo == null) {
                    opsInfo = new OpsInfo();
                    opsInfo.op = op;
                    opsInfo.mode = mode;
                    opss.put(opSwitch, opsInfo);
                }
                if (time >= rejectTime) {
                    if (time > opsInfo.time) {
                        opsInfo.allow = true;
                        opsInfo.time = time;
                    }
                } else {
                    if (rejectTime > opsInfo.time) {
                        opsInfo.allow = false;
                        opsInfo.time = rejectTime;
                    }
                }
            }
        }
        return opss;
    }

    private static int getPackageUid(String packageName, int uid) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return getPackageManager().getPackageUid(packageName,
                        PackageManager.MATCH_UNINSTALLED_PACKAGES, uid);
            } else {
                return getPackageUidDeprecated(packageName, uid);
            }
        } catch (RemoteException | RuntimeException e) {
            UILog.w("Can't getPackageUid for " + packageName, e);
            return 0;
        }
    }

    @SuppressWarnings("deprecation")
    private static int getPackageUidDeprecated(String packageName, int uid) throws RemoteException {
        return getPackageManager().getPackageUid(packageName, uid);
    }

    private static IPackageManager getPackageManager() {
        return IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    }

    public int getSelectedSize() {
        return mSelected.size();
    }

    public void clearSelected() {
        mSelected.clear();
        getActivity().updateSelected(mSelected.size());
    }

    public void selectInverse() {
        for (OpsInfo opsInfo : mOpsInfo) {
            if (!mSelected.remove(opsInfo.op)) {
                mSelected.add(opsInfo.op);
            }
        }
        getActivity().updateSelected(mSelected.size());
    }

    public void selectAll() {
        for (OpsInfo opsInfo : mOpsInfo) {
            mSelected.add(opsInfo.op);
        }
        getActivity().updateSelected(mSelected.size());
    }

    public boolean canAllow() {
        if (mSelected.isEmpty()) {
            return false;
        }
        for (OpsInfo opsInfo : mOpsInfo) {
            if (mSelected.contains(opsInfo.op) && opsInfo.mode != AppOpsManager.MODE_ALLOWED) {
                return true;
            }
        }
        return false;
    }

    public boolean canIgnore() {
        if (mSelected.isEmpty()) {
            return false;
        }
        for (OpsInfo opsInfo : mOpsInfo) {
            if (mSelected.contains(opsInfo.op) && opsInfo.mode != AppOpsManager.MODE_IGNORED) {
                return true;
            }
        }
        return false;
    }

    public Collection<Integer> getSelected() {
        return mSelected;
    }

    private static class DiffCallback extends DiffUtil.Callback {

        private final List<OpsInfo> mOldList;

        private final List<OpsInfo> mNewList;

        DiffCallback(List<OpsInfo> oldList, List<OpsInfo> newList) {
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
            return mOldList.get(oldItemPosition) == mNewList.get(newItemPosition);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            OpsInfo oldInfo = mOldList.get(oldItemPosition);
            OpsInfo newInfo = mNewList.get(newItemPosition);
            return oldInfo == newInfo && !oldInfo.updated;
        }

    }

}
