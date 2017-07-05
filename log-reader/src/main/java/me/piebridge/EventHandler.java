package me.piebridge;

import android.util.EventLog;

/**
 * EventHandler for LogReader
 * <p>
 * Created by thom on 2017/1/22.
 */
@SuppressWarnings("unused")
public interface EventHandler {

    /**
     * Filter event using tag
     * <p>
     * This is useful to avoid new instance of Event.
     *
     * @param tag tag of Event
     * @return acceptable
     * @see android.util.EventLog.Event#getTag
     */
    boolean accept(int tag);

    /**
     * parse event
     *
     * @param event Event
     * @return false to exit readEvents
     * @see LogReader#readEvents(EventHandler)
     */
    boolean onEvent(EventLog.Event event);

}
