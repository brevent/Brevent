package me.piebridge.brevent.server;

import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.v4.util.SimpleArrayMap;
import android.util.EventLog;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import me.piebridge.EventHandler;
import me.piebridge.LogReader;
import me.piebridge.brevent.BuildConfig;

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

    BreventEvent(Handler handler, CountDownLatch countDownLatch) throws IOException {
        mHandler = handler;
        mCountDownLatch = countDownLatch;
        mEventTag = new EventTag();
        mUser = HideApi.getCurrentUser();
        amSwitchUserTag = mEventTag.getTag(EventTag.AM_SWITCH_USER);
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        LogReader.readEvents(HideApi.getSystemPid(), this);
        ServerLog.d("Brevent Event countDown");
        mCountDownLatch.countDown();
    }

    @Override
    public boolean accept(int tag) {
        return quit || tag == amSwitchUserTag || (mUser == HideApi.USER_OWNER && mEventTag.contains(tag));
    }

    @Override
    public boolean onEvent(EventLog.Event event) {
        if (quit) {
            return !quit;
        }
        int tag = event.getTag();
        int eventId = mEventTag.getEvent(tag);
        SimpleArrayMap<String, Object> events = convertData(tag, event);
        if (!BuildConfig.RELEASE) {
            ServerLog.d(EventTag.getEventName(eventId) + ": " + events);
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
