package me.piebridge.brevent.ui;

import android.app.Application;

import java.util.UUID;

/**
 * Created by thom on 2017/2/7.
 */
public class BreventApplication extends Application {

    private UUID mToken;

    boolean started;

    @Override
    public void onCreate() {
        super.onCreate();
        mToken = UUID.randomUUID();
    }

    public UUID getToken() {
        return mToken;
    }

}
