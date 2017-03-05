package me.piebridge.brevent.ui;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventPackages;

/**
 * Created by thom on 2017/2/3.
 */
public class AppsActivityUIHandler extends Handler {

    private final WeakReference<BreventActivity> mReference;

    public AppsActivityUIHandler(BreventActivity activity) {
        super(activity.getMainLooper());
        mReference = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message message) {
        BreventActivity activity = mReference.get();
        if (activity != null) {
            switch (message.what) {
                case BreventActivity.UI_MESSAGE_SHOW_PROGRESS:
                    activity.showProgress(R.string.process_retrieving);
                    break;
                case BreventActivity.UI_MESSAGE_HIDE_PROGRESS:
                    activity.hideProgress();
                case BreventActivity.UI_MESSAGE_HIDE_DISABLED:
                    activity.hideDisabled();
                    break;
                case BreventActivity.UI_MESSAGE_SHOW_PAGER:
                    activity.showViewPager();
                    break;
                case BreventActivity.UI_MESSAGE_SHOW_FRAGMENT:
                    ((AppsFragment) message.obj).show();
                    break;
                case BreventActivity.UI_MESSAGE_NO_BREVENT:
                    activity.showDisabled();
                    break;
                case BreventActivity.UI_MESSAGE_IO_BREVENT:
                    // FIXME
                    activity.showDisabled();
                    break;
                case BreventActivity.UI_MESSAGE_NO_BREVENT_DATA:
                case BreventActivity.UI_MESSAGE_VERSION_UNMATCHED:
                    activity.showDisabled(R.string.brevent_service_restart);
                    break;
                case BreventActivity.UI_MESSAGE_UPDATE_BREVENT:
                    activity.updateBreventResponse((BreventPackages) message.obj);
                    break;
            }
        }
    }

}