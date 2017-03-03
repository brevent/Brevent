package me.piebridge.brevent.server;

/**
 * Created by thom on 2016/12/30.
 */

class TaskRecord {

    int stack;
    boolean top;
    Integer userId;
    String packageName;
    int inactive;
    String state;

    boolean isHome() {
        return stack == 0;
    }

}