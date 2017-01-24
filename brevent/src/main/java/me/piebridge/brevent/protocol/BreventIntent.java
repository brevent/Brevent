package me.piebridge.brevent.protocol;

import me.piebridge.brevent.Manifest;

/**
 * Created by thom on 15/7/12.
 */
public final class BreventIntent {

    public static final String ACTION_BREVENT = "me.piebridge.brevent.intent.action.BREVENT";

    public static final String PERMISSION_MANAGER = Manifest.permission.MANAGER;
    public static final String PERMISSION_SHELL = "android.permission.DEVICE_POWER";

    private BreventIntent() {

    }

}
