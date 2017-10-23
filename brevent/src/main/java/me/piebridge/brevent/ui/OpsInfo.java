package me.piebridge.brevent.ui;

import android.text.TextUtils;

import java.text.Collator;
import java.util.Comparator;

/**
 * Created by thom on 2017/10/21.
 */
class OpsInfo {

    int op;

    String name;

    String permission = "";

    String label = "";

    String groupLabel = "";

    long time;

    int mode;

    int icon;

    boolean allow;

    boolean updated;

    void update(OpsInfo opsInfo) {
        if (opsInfo != null && opsInfo.op == op) {
            if (opsInfo.allow != allow) {
                allow = opsInfo.allow;
                updated = true;
            }
            if (opsInfo.mode != mode) {
                mode = opsInfo.mode;
                updated = true;
            }
            if (opsInfo.time != time) {
                time = opsInfo.time;
                updated = true;
            }
        }
    }

    static class SortByOp implements Comparator<OpsInfo> {

        @Override
        public int compare(OpsInfo o1, OpsInfo o2) {
            return Integer.compare(o1.op, o2.op);
        }

    }

    static class SortByName implements Comparator<OpsInfo> {

        @Override
        public int compare(OpsInfo o1, OpsInfo o2) {
            return Collator.getInstance().compare(o1.name, o2.name);
        }

    }

    static class SortByTime extends SortByName {

        @Override
        public int compare(OpsInfo o1, OpsInfo o2) {
            int result = Long.compare(o2.time, o1.time);
            if (result == 0) {
                result = super.compare(o1, o2);
            }
            return result;
        }

    }

    static class SortByGroup extends SortByTime {

        @Override
        public int compare(OpsInfo o1, OpsInfo o2) {
            int result;
            if (TextUtils.isEmpty(o1.groupLabel)) {
                result = TextUtils.isEmpty(o2.groupLabel) ? 0 : 1;
            } else if (TextUtils.isEmpty(o2.groupLabel)) {
                result = -1;
            } else {
                result = Collator.getInstance().compare(o1.groupLabel, o2.groupLabel);
            }
            if (result == 0) {
                result = super.compare(o1, o2);
            }
            return result;
        }

    }

    static class SortByMode extends SortByTime {

        @Override
        public int compare(OpsInfo o1, OpsInfo o2) {
            int result = Integer.compare(o1.mode, o2.mode);
            if (result == 0) {
                result = super.compare(o1, o2);
            }
            return result;
        }

    }

}
