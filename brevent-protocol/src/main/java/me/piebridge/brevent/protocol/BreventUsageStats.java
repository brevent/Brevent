package me.piebridge.brevent.protocol;

import android.app.usage.UsageStats;
import android.os.Parcel;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;

/**
 * Created by thom on 2018/2/28.
 */
public class BreventUsageStats {

    private final long mLastTimeUsed;

    private final long mTotalTimeInForeground;

    private BreventUsageStats(UsageStats stats) {
        mLastTimeUsed = stats.getLastTimeUsed();
        mTotalTimeInForeground = stats.getTotalTimeInForeground();
    }

    BreventUsageStats(Parcel in) {
        mLastTimeUsed = in.readLong();
        mTotalTimeInForeground = in.readLong();
    }

    public void writeToParcel(Parcel dest) {
        dest.writeLong(mLastTimeUsed);
        dest.writeLong(mTotalTimeInForeground);
    }

    public long getLastTimeUsed() {
        return mLastTimeUsed;
    }

    public long getTotalTimeInForeground() {
        return mTotalTimeInForeground;
    }

    public static SimpleArrayMap<String, BreventUsageStats> convert(SimpleArrayMap<String, UsageStats> map) {
        if (map == null) {
            return new SimpleArrayMap<>();
        }
        SimpleArrayMap<String, BreventUsageStats> breventStats = new ArrayMap<>();
        int size = map.size();
        for (int i = 0; i < size; ++i) {
            String key = map.keyAt(i);
            UsageStats usageStats = map.valueAt(i);
            breventStats.put(key, new BreventUsageStats(usageStats));
        }
        return breventStats;
    }

}
