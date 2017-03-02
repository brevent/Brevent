package me.piebridge.brevent.server;

import android.app.ActivityManager;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.v4.util.SimpleArrayMap;
import android.util.EventLog;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import me.piebridge.EventHandler;
import me.piebridge.LogReader;

/**
 * Event handler
 * Created by thom on 2017/2/13.
 */
class BreventEvent implements Runnable, EventHandler {

    private volatile boolean quit;

    private final Handler mHandler;

    private final CountDownLatch mCountDownLatch;

    private final EventTag mEventTag;

    private int mUser;

    private final int amSwitchUserTag;

    private int mPid;

    BreventEvent(Handler handler, CountDownLatch countDownLatch) throws IOException {
        mHandler = handler;
        mCountDownLatch = countDownLatch;
        mEventTag = new EventTag();
        mUser = HideApi.getCurrentUser();
        mPid = getSystemPid(HideApi.getRunningAppProcesses());
        amSwitchUserTag = mEventTag.getTag(EventTag.AM_SWITCH_USER);
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        LogReader.readEvents(0, this);
        ServerLog.d("Brevent Event countDown");
        mCountDownLatch.countDown();
    }

    public static int getSystemPid(List<ActivityManager.RunningAppProcessInfo> processes) {
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            for (String packageName : process.pkgList) {
                if ("android".equals(packageName)) {
                    return process.pid;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean accept(int tag) {
        return quit || tag == EventTag.TAG_ANSWER || tag == amSwitchUserTag || (mUser == HideApi.USER_OWNER && mEventTag.contains(tag));
    }

    @Override
    public boolean onEvent(EventLog.Event event) {
        if (quit) {
            return false;
        }
        int tag = event.getTag();
        if (tag == EventTag.TAG_ANSWER) {
            if (Objects.equals(event.getData(), 0xfee1900d)) {
                ServerLog.d("received feelgood");
                mHandler.removeMessages(BreventServer.MESSAGE_DEAD);
            }
            return true;
        }
        if (mPid != event.getProcessId()) {
            return true;
        }
        int eventId = mEventTag.getEvent(tag);
        SimpleArrayMap<String, Object> events = convertData(tag, event);
        if (Log.isLoggable(ServerLog.TAG, Log.VERBOSE)) {
            ServerLog.v(EventTag.getEventName(eventId) + ": " + events);
        }
        if (tag == amSwitchUserTag) {
            // am_switch_user (id|1|5)
            mUser = (int) events.get("id");
        } else {
            Message message = mHandler.obtainMessage(BreventServer.MESSAGE_EVENT);
            message.arg1 = eventId;
            message.arg2 = event.getThreadId();
            message.obj = events;
            mHandler.sendMessage(message);
        }
        return true;
    }

    private SimpleArrayMap<String, Object> convertData(int tag, EventLog.Event event) {
        Object data = event.getData();
        if (data.getClass().isArray()) {
            return mEventTag.buildEvents(tag, (Object[]) data);
        } else {
            return mEventTag.buildEvents(tag, new Object[] {data});
        }
    }

    void quit() {
        quit = true;
    }

}
