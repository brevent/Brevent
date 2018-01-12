package me.piebridge.brevent.ui;

import android.util.Log;

/**
 * Created by thom on 15/7/25.
 */
class UILog {

    static final String TAG = "BreventUI";

    private UILog() {

    }

    static void d(String msg) {
        Log.d(TAG, msg);
    }

    static void i(String msg) {
        Log.i(TAG, msg);
    }

    static void i(String msg, Throwable t) {
        Log.i(TAG, msg, t);
    }

    static void w(String msg) {
        Log.w(TAG, msg);
    }

    static void w(String msg, Throwable t) {
        Log.w(TAG, msg, t);
    }

    static void e(String msg, Throwable t) {
        Log.e(TAG, msg, t);
    }

    static void e(String msg) {
        Log.e(TAG, msg);
    }

}
