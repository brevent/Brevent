package me.piebridge.brevent.server;

import android.util.Log;

/**
 * Created by thom on 2017/2/12.
 */
class ServerLog {

    private static final String TAG = "BreventServer";

    static void v(String msg) {
        Log.v(TAG, msg);
    }

    static void v(String msg, Throwable t) {
        Log.e(TAG, msg);
    }

    static void d(String msg) {
        Log.d(TAG, msg);
    }

    static void d(String msg, Throwable t) {
        Log.d(TAG, msg);
    }

    static void i(String msg) {
        Log.i(TAG, msg);
    }

    static void i(String msg, Throwable t) {
        Log.i(TAG, msg);
    }

    static void w(String msg) {
        Log.e(TAG, msg);
    }

    static void w(String msg, Throwable t) {
        Log.e(TAG, msg, t);
    }

    static void e(String msg, Throwable t) {
        Log.e(TAG, msg, t);
    }

    static void e(String msg) {
        Log.e(TAG, msg);
    }

}
