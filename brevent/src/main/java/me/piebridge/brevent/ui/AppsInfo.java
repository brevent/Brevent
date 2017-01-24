package me.piebridge.brevent.ui;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.text.Collator;

/**
 * Created by thom on 2017/1/25.
 */
class AppsInfo implements Comparable<AppsInfo> {

    private static final String EMPTY = "";

    static final int STATUS_UNKNOWN = -1;

    static final int STATUS_RUNNING = 0;

    static final int STATUS_STANDBY = 1;

    static final int STATUS_STOPPED = 2;

    String packageName = EMPTY;

    String label = EMPTY;

    int status = STATUS_UNKNOWN;

    AppsInfo(String packageName, String label) {
        this.packageName = packageName;
        this.label = label;
    }

    AppsInfo(Integer status, String label) {
        this.status = status;
        this.label = label;
    }

    @Override
    public int compareTo(@NonNull AppsInfo another) {
        if (status < another.status) {
            return -1;
        } else if (status > another.status) {
            return 1;
        } else if (TextUtils.isEmpty(packageName)) {
            return -1;
        } else if (TextUtils.isEmpty(another.packageName)) {
            return 1;
        } else {
            return Collator.getInstance().compare(label, another.label);
        }
    }

    public boolean isPackage() {
        return !EMPTY.equals(packageName);
    }

}
