package me.piebridge.brevent.ui;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;

/**
 * Created by thom on 2017/2/1.
 */
public class AppsScrollListener extends RecyclerView.OnScrollListener {

    private final Handler mHandler;

    AppsScrollListener(Handler handler) {
        mHandler = handler;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        mHandler.removeMessages(AppsItemHandler.MSG_UPDATE_TIME);
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            mHandler.sendEmptyMessage(AppsItemHandler.MSG_UPDATE_ITEM);
        }
    }

}
