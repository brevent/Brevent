package me.piebridge.brevent.ui;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventPackages;
import me.piebridge.brevent.protocol.BreventPriority;

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
        if (activity != null && !activity.isStopped()) {
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
                    activity.showDisabled(R.string.brevent_service_no_response);
                    break;
                case BreventActivity.UI_MESSAGE_UPDATE_BREVENT:
                    activity.updateBreventResponse((BreventPackages) message.obj);
                    break;
                case BreventActivity.UI_MESSAGE_UPDATE_PRIORITY:
                    activity.updateBreventResponse((BreventPriority) message.obj);
                    break;
                case BreventActivity.UI_MESSAGE_SHOW_SUCCESS:
                    activity.showSuccess();
                    break;
                case BreventActivity.UI_MESSAGE_NO_EVENT:
                    activity.showNoEvent();
                    break;
                case BreventActivity.UI_MESSAGE_MAKE_EVENT:
                    activity.makeEvent();
                    break;
                case BreventActivity.UI_MESSAGE_NO_PERMISSION:
                    activity.showNoPermission();
                    break;
                default:
                    break;
            }
        }
    }

}