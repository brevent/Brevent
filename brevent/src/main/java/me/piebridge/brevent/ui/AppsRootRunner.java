package me.piebridge.brevent.ui;

import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by thom on 2017/4/1.
 */

public class AppsRootRunner implements Runnable {

    private static ThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private final String path;

    private final Handler handler;

    public AppsRootRunner(String path, Handler handler) {
        this.path = path;
        this.handler = handler;
    }

    public void submit() {
        executor.submit(this);
    }

    private boolean startBrevent() {
        UILog.d("startBrevent: " + path);
        List<String> results = Shell.SU.run(path);
        if (results != null) {
            for (String result : results) {
                UILog.d(result);
            }
            return true;
        } else {
            UILog.d("Can't run as root");
            handler.sendEmptyMessage(BreventActivity.UI_MESSAGE_NO_BREVENT);
            return false;
        }
    }

    @Override
    public void run() {
        startBrevent();
    }

}
