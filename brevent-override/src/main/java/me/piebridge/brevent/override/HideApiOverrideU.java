package me.piebridge.brevent.override;

import android.app.UiAutomation;
import android.app.UiAutomationConnection;
import android.os.HandlerThread;

/**
 * Created by thom on 2017/5/28.
 */
public class HideApiOverrideU {

    private static final String HANDLER_THREAD_NAME = "UiAutomatorHandlerThread";

    private final HandlerThread mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);

    private UiAutomation mUiAutomation;

    public void connect() {
        if (mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already connected!");
        }
        mHandlerThread.start();
        mUiAutomation = new UiAutomation(mHandlerThread.getLooper(),
                new UiAutomationConnection());
        try {
            mUiAutomation.disconnect();
        } catch (RuntimeException e) {
            // do nothing
        }
        mUiAutomation.connect();
    }

    public void disconnect() {
        if (!mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already disconnected!");
        }
        try {
            mUiAutomation.disconnect();
        } finally {
            mHandlerThread.quit();
        }
    }

    public UiAutomation getUiAutomation() {
        return mUiAutomation;
    }

}
