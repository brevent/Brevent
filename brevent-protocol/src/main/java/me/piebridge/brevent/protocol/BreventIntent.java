package me.piebridge.brevent.protocol;

/**
 * Created by thom on 15/7/12.
 */
public final class BreventIntent {

    public static final String ACTION_HOME_TID = "me.piebridge.brevent.intent.action.HOME_TID";
    public static final String ACTION_ADD_PACKAGE = "me.piebridge.brevent.intent.action.ADD_PACKAGE";
    public static final String ACTION_RUN_AS_ROOT = "me.piebridge.brevent.intent.action.RUN_AS_ROOT";

    public static final String PERMISSION_MANAGER = "me.piebridge.brevent.permission.MANAGER";

    public static final String EXTRA_HOME_TID = "me.piebridge.brevent.intent.extra.HOME_TID";
    public static final String EXTRA_BREVENT_SIZE = "me.piebridge.brevent.intent.extra.BREVENT_SIZE";

    private BreventIntent() {

    }

}
