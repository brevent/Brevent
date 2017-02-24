package me.piebridge.brevent.ui;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.UiThread;
import android.support.v4.util.ArraySet;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import me.piebridge.brevent.protocol.TimeUtils;

/**
 * handler for apps item
 * Created by thom on 2017/2/2.
 */
@UiThread
public class AppsItemHandler extends Handler {

    private static final int TIME_UPDATE_DELAY = 1000;

    public static final int MSG_UPDATE_TIME = 0;
    public static final int MSG_UPDATE_ITEM = 1;
    public static final int MSG_RETRIEVE_PACKAGES = 2;
    public static final int MSG_STOP_UPDATE = 3;

    private final AppsFragment mFragment;

    private final RecyclerView mRecyclerView;

    public AppsItemHandler(AppsFragment fragment, RecyclerView recyclerView) {
        super(fragment.getActivity().getMainLooper());
        mFragment = fragment;
        mRecyclerView = recyclerView;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_UPDATE_ITEM:
            case MSG_UPDATE_TIME:
                @SuppressWarnings("unchecked")
                Set<Integer> positions = (Set<Integer>) msg.obj;
                if (positions == null) {
                    positions = getInactivePositions();
                }
                updateInactive(positions);
                break;
            case MSG_RETRIEVE_PACKAGES:
                mFragment.retrievePackages();
                break;
            case MSG_STOP_UPDATE:
                removeMessages(MSG_UPDATE_ITEM);
                removeMessages(MSG_UPDATE_TIME);
                sendEmptyMessageDelayed(MSG_UPDATE_ITEM, TIME_UPDATE_DELAY);
                break;
            default:
                break;
        }
    }

    private void updateInactive(Set<Integer> positions) {
        removeMessages(MSG_UPDATE_TIME);
        if (!positions.isEmpty() && updateInactiveTime(positions)) {
            Message message = obtainMessage(MSG_UPDATE_TIME, positions);
            sendMessageDelayed(message, TIME_UPDATE_DELAY);
        }
    }

    private boolean updateInactiveTime(Set<Integer> positions) {
        final int now = TimeUtils.now();
        for (int position : positions) {
            AppsItemViewHolder viewHolder = getViewHolder(position);
            if (viewHolder != null) {
                viewHolder.inactiveView.setText(DateUtils.formatElapsedTime(now - viewHolder.inactive));
            }
        }
        return true;
    }

    private AppsItemViewHolder getViewHolder(int position) {
        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
        if (holder instanceof AppsItemViewHolder) {
            return (AppsItemViewHolder) holder;
        } else {
            return null;
        }

    }

    private Set<Integer> getInactivePositions() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        AppsItemAdapter adapter = (AppsItemAdapter) mRecyclerView.getAdapter();
        if (adapter == null || layoutManager == null) {
            return Collections.emptySet();
        }
        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();
        if (first == -1 || last == -1) {
            return Collections.emptySet();
        }
        List<AppsInfo> appsInfoList = adapter.getAppsInfo();
        BreventActivity activity = (BreventActivity) mFragment.getActivity();
        if (appsInfoList.isEmpty() || activity == null) {
            return Collections.emptySet();
        }
        Set<Integer> positions = new ArraySet<>();
        final int now = TimeUtils.now();
        last = Math.min(last, appsInfoList.size());
        for (int i = first; i <= last; ++i) {
            AppsInfo info = appsInfoList.get(i);
            if (info.isPackage()) {
                int inactive = activity.getInactive(info.packageName);
                if (inactive > 0) {
                    positions.add(i);
                }
                AppsItemViewHolder viewHolder = getViewHolder(i);
                if (viewHolder != null) {
                    viewHolder.inactive = inactive;
                    viewHolder.inactiveView.setText(inactive > 0 ? DateUtils.formatElapsedTime(now - inactive) : null);
                }
            }
        }
        return positions;
    }

}
