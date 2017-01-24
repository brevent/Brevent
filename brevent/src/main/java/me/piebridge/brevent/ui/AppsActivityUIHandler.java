package me.piebridge.brevent.ui;

import android.os.Handler;
import android.os.Message;

import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventPackages;

/**
 * Created by thom on 2017/2/3.
 */
public class AppsActivityUIHandler extends Handler {

    private final BreventActivity mActivity;

    public AppsActivityUIHandler(BreventActivity breventActivity) {
        super(breventActivity.getMainLooper());
        mActivity = breventActivity;
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case BreventActivity.UI_MESSAGE_SHOW_PROGRESS:
                mActivity.showProgress(R.string.process_retrieving);
                break;
            case BreventActivity.UI_MESSAGE_HIDE_PROGRESS:
                mActivity.hideDisabled();
                mActivity.hideProgress();
                break;
            case BreventActivity.UI_MESSAGE_SHOW_PAGER:
                mActivity.showViewPager();
                break;
            case BreventActivity.UI_MESSAGE_SHOW_FRAGMENT:
                ((AppsFragment) message.obj).show();
                break;
            case BreventActivity.UI_MESSAGE_NO_BREVENT:
                mActivity.showDisabled();
                break;
            case BreventActivity.UI_MESSAGE_IO_BREVENT:
                // FIXME
                mActivity.showDisabled();
                break;
            case BreventActivity.UI_MESSAGE_NO_BREVENT_DATA:
            case BreventActivity.UI_MESSAGE_VERSION_UNMATCHED:
                mActivity.showDisabled(R.string.brevent_service_restart);
                break;
            case BreventActivity.UI_MESSAGE_UPDATE_BREVENT:
                mActivity.updateBreventResponse((BreventPackages) message.obj);
                break;
        }
    }

}