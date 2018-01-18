package me.piebridge.brevent.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.Collections;

import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/10/21.
 */
public class OpsFragment extends Fragment {

    private RecyclerView mRecycler;

    public OpsFragment() {
        setArguments(new Bundle());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_ops, container, false);
        mRecycler = mView.findViewById(R.id.recycler);
        return mView;
    }

    @Override
    public void onDestroyView() {
        mRecycler = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        String packageName = getArguments().getString(Intent.EXTRA_PACKAGE_NAME);
        if (mRecycler != null && mRecycler.getAdapter() == null && !TextUtils.isEmpty(packageName)) {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
            linearLayoutManager.setAutoMeasureEnabled(true);
            mRecycler.setLayoutManager(linearLayoutManager);
            mRecycler.addItemDecoration(new DividerItemDecoration(mRecycler.getContext(),
                    LinearLayoutManager.VERTICAL));
            mRecycler.setAdapter(new OpsItemAdapter(this, packageName));
        }
    }

    public void updateSort() {
        OpsItemAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.sort();
        }
    }

    public int getSelectedSize() {
        OpsItemAdapter adapter = getAdapter();
        if (adapter != null) {
            return adapter.getSelectedSize();
        } else {
            return 0;
        }
    }

    public void clearSelected() {
        OpsItemAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.clearSelected();
        }
    }

    public void selectInverse() {
        OpsItemAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.selectInverse();
        }
    }

    public void selectAll() {
        OpsItemAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.selectAll();
        }
    }

    public void updateSelected() {
        OpsItemAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        }
    }

    public boolean canAllow() {
        OpsItemAdapter adapter = getAdapter();
        if (adapter != null) {
            return adapter.canAllow();
        } else {
            return false;
        }
    }

    public boolean canIgnore() {
        OpsItemAdapter adapter = getAdapter();
        if (adapter != null) {
            return adapter.canIgnore();
        } else {
            return false;
        }
    }

    public Collection<Integer> getOps() {
        OpsItemAdapter adapter = getAdapter();
        if (adapter != null) {
            return adapter.getSelected();
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public void refresh() {
        OpsItemAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.refresh();
        }
    }

    private OpsItemAdapter getAdapter() {
        if (mRecycler != null) {
            return (OpsItemAdapter) mRecycler.getAdapter();
        } else {
            return null;
        }
    }

}
