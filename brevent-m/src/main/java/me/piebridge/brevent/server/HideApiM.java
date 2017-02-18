package me.piebridge.brevent.server;

import android.app.IActivityManager;

import java.util.List;

/**
 * Created by thom on 2017/2/17.
 */
public class HideApiM {

    private HideApiM() {

    }

    @SuppressWarnings("unchecked")
    static List getRecentTasks(IActivityManager am, int maxNum, int flags, int userId) {
        return am.getRecentTasks(maxNum, flags, userId);
    }

}
