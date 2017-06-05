package me.piebridge.brevent.ui;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.text.Collator;
import java.util.Objects;

/**
 * Created by thom on 2017/1/25.
 */
class AppsInfo implements Comparable<AppsInfo> {

    private static final int PRIME_HASH = 59;

    private static final int PRIME_NULL = 43;

    private static final String EMPTY = "";

    private static final int STATUS_UNKNOWN = -1;

    static final int STATUS_RUNNING = 0;

    static final int STATUS_STANDBY = 1;

    static final int STATUS_STOPPED = 2;

    String packageName = EMPTY;

    String label = EMPTY;

    int status = STATUS_UNKNOWN;

    boolean updated = false;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof AppsInfo) {
            AppsInfo other = (AppsInfo) obj;
            return status == other.status
                    && Objects.equals(label, other.label)
                    && Objects.equals(packageName, other.packageName);

        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = PRIME_HASH + (packageName == null ? PRIME_NULL : packageName.hashCode());
        result = result * PRIME_HASH + (label == null ? PRIME_NULL : label.hashCode());
        result = result * PRIME_HASH + status;
        return result;
    }

    public boolean isPackage() {
        return !EMPTY.equals(packageName);
    }

}
