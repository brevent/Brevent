package me.piebridge.brevent.server;

import android.app.IActivityManager;
import android.os.UserHandle;

import java.util.List;

/**
 * Created by thom on 2017/2/22.
 */
public class HideApiOverrideM {

    public static final int USER_OWNER = UserHandle.USER_OWNER;

    private HideApiOverrideM() {

    }

    @SuppressWarnings("unchecked")
    static List getRecentTasks(IActivityManager am, int maxNum, int flags, int userId) {
        return am.getRecentTasks(maxNum, flags, userId);
    }

}
