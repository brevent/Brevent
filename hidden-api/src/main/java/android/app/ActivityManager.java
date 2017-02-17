package android.app;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.List;

/**
 * Activity Manager
 * Created by thom on 2017/2/7.
 */
public class ActivityManager {

    /** @hide Process does not exist. */
    @RequiresApi(Build.VERSION_CODES.M)
    public static int PROCESS_STATE_NONEXISTENT = -1;

    /** @hide Process is a persistent system process. */
    public static int PROCESS_STATE_PERSISTENT = 0;

    /** @hide Process is a persistent system process and is doing UI. */
    public static int PROCESS_STATE_PERSISTENT_UI = 1;

    /** @hide Process is hosting the current top activities.  Note that this covers
     * all activities that are visible to the user. */
    public static int PROCESS_STATE_TOP = 2;

    /** @hide Process is hosting a foreground service due to a system binding. */
    @RequiresApi(Build.VERSION_CODES.M)
    public static int PROCESS_STATE_BOUND_FOREGROUND_SERVICE = 3;

    /** @hide Process is hosting a foreground service. */
    @RequiresApi(Build.VERSION_CODES.M)
    public static int PROCESS_STATE_FOREGROUND_SERVICE = 4;

    /** @hide Same as {@link #PROCESS_STATE_TOP} but while device is sleeping. */
    @RequiresApi(Build.VERSION_CODES.M)
    public static int PROCESS_STATE_TOP_SLEEPING = 5;

    /** @hide Process is important to the user, and something they are aware of. */
    public static int PROCESS_STATE_IMPORTANT_FOREGROUND = 6;

    /** @hide Process is important to the user, but not something they are aware of. */
    public static int PROCESS_STATE_IMPORTANT_BACKGROUND = 7;

    /** @hide Process is in the background running a backup/restore operation. */
    public static int PROCESS_STATE_BACKUP = 8;

    /** @hide Process is in the background, but it can't restore its state so we want
     * to try to avoid killing it. */
    public static int PROCESS_STATE_HEAVY_WEIGHT = 9;

    /** @hide Process is in the background running a service.  Unlike oom_adj, this level
     * is used for both the normal running in background state and the executing
     * operations state. */
    public static int PROCESS_STATE_SERVICE = 10;

    /** @hide Process is in the background running a receiver.   Note that from the
     * perspective of oom_adj receivers run at a higher foreground level, but for our
     * prioritization here that is not necessary and putting them below services means
     * many fewer changes in some process states as they receive broadcasts. */
    public static int PROCESS_STATE_RECEIVER = 11;

    /** @hide Process is in the background but hosts the home activity. */
    public static int PROCESS_STATE_HOME = 12;

    /** @hide Process is in the background but hosts the last shown activity. */
    public static int PROCESS_STATE_LAST_ACTIVITY = 13;

    /** @hide Process is being cached for later use and contains activities. */
    public static int PROCESS_STATE_CACHED_ACTIVITY = 14;

    /** @hide Process is being cached for later use and is a client of another cached
     * process that contains activities. */
    public static int PROCESS_STATE_CACHED_ACTIVITY_CLIENT = 15;

    /** @hide Process is being cached for later use and is empty. */
    public static int PROCESS_STATE_CACHED_EMPTY = 16;

    /** @hide The lowest process state number */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static int MIN_PROCESS_STATE = PROCESS_STATE_NONEXISTENT;

    /** @hide The highest process state number */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static int MAX_PROCESS_STATE = PROCESS_STATE_CACHED_EMPTY;

    /** @hide Should this process state be considered a background state? */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean isProcStateBackground(int procState) {
        throw new UnsupportedOperationException();
    }

    public static int getCurrentUser() {
        throw new UnsupportedOperationException();
    }

    /**
     * Information you can retrieve about a running process.
     */
    public static class RunningAppProcessInfo {
        /**
         * The name of the process that this object is associated with
         */
        public String processName;

        /**
         * The pid of this process; 0 if none
         */
        public int pid;

        /**
         * The user id of this process.
         */
        public int uid;

        /**
         * All packages that have been loaded into the process.
         */
        public String pkgList[];

        /**
         * Constant for {@link #flags}: this is an app that is unable to
         * correctly save its state when going to the background,
         * so it can not be killed while in the background.
         * @hide
         */
        public static int FLAG_CANT_SAVE_STATE = 1<<0;

        /**
         * Constant for {@link #flags}: this process is associated with a
         * persistent system app.
         * @hide
         */
        public static int FLAG_PERSISTENT = 1<<1;

        /**
         * Constant for {@link #flags}: this process is associated with a
         * persistent system app.
         * @hide
         */
        public static int FLAG_HAS_ACTIVITIES = 1<<2;

        /**
         * Flags of information.  May be any of
         * {@link #FLAG_CANT_SAVE_STATE}.
         * @hide
         */
        public int flags;

        /**
         * Last memory trim level reported to the process: corresponds to
         * the values supplied to {@link android.content.ComponentCallbacks2#onTrimMemory(int)
         * ComponentCallbacks2.onTrimMemory(int)}.
         */
        public int lastTrimLevel;

        /**
         * Constant for {@link #importance}: This process is running the
         * foreground UI; that is, it is the thing currently at the top of the screen
         * that the user is interacting with.
         */
        public static int IMPORTANCE_FOREGROUND = 100;

        /**
         * Constant for {@link #importance}: This process is running a foreground
         * service, for example to perform music playback even while the user is
         * not immediately in the app.  This generally indicates that the process
         * is doing something the user actively cares about.
         */
        public static int IMPORTANCE_FOREGROUND_SERVICE = 125;

        /**
         * Constant for {@link #importance}: This process is running the foreground
         * UI, but the device is asleep so it is not visible to the user.  This means
         * the user is not really aware of the process, because they can not see or
         * interact with it, but it is quite important because it what they expect to
         * return to once unlocking the device.
         */
        public static int IMPORTANCE_TOP_SLEEPING = 150;

        /**
         * Constant for {@link #importance}: This process is running something
         * that is actively visible to the user, though not in the immediate
         * foreground.  This may be running a window that is behind the current
         * foreground (so paused and with its state saved, not interacting with
         * the user, but visible to them to some degree); it may also be running
         * other services under the system's control that it inconsiders important.
         */
        public static int IMPORTANCE_VISIBLE = 200;

        /**
         * Constant for {@link #importance}: This process is not something the user
         * is directly aware of, but is otherwise perceptable to them to some degree.
         */
        public static int IMPORTANCE_PERCEPTIBLE = 130;

        /**
         * Constant for {@link #importance}: This process is running an
         * application that can not save its state, and thus can't be killed
         * while in the background.
         * @hide
         */
        public static int IMPORTANCE_CANT_SAVE_STATE = 170;

        /**
         * Constant for {@link #importance}: This process is contains services
         * that should remain running.  These are background services apps have
         * started, not something the user is aware of, so they may be killed by
         * the system relatively freely (though it is generally desired that they
         * stay running as long as they want to).
         */
        public static int IMPORTANCE_SERVICE = 300;

        /**
         * Constant for {@link #importance}: This process process contains
         * background code that is expendable.
         */
        public static int IMPORTANCE_BACKGROUND = 400;

        /**
         * Constant for {@link #importance}: This process is empty of any
         * actively running code.
         */
        public static int IMPORTANCE_EMPTY = 500;

        /**
         * Constant for {@link #importance}: This process does not exist.
         */
        public static int IMPORTANCE_GONE = 1000;

        /** @hide */
        public static int procStateToImportance(int procState) {
            throw new UnsupportedOperationException();
        }

        /**
         * The relative importance level that the system places on this
         * process.  May be one of {@link #IMPORTANCE_FOREGROUND},
         * {@link #IMPORTANCE_VISIBLE}, {@link #IMPORTANCE_SERVICE},
         * {@link #IMPORTANCE_BACKGROUND}, or {@link #IMPORTANCE_EMPTY}.  These
         * constants are numbered so that "more important" values are always
         * smaller than "less important" values.
         */
        public int importance;

        /**
         * An additional ordering within a particular {@link #importance}
         * category, providing finer-grained information about the relative
         * utility of processes within a category.  This number means nothing
         * except that a smaller values are more recently used (and thus
         * more important).  Currently an LRU value is only maintained for
         * the {@link #IMPORTANCE_BACKGROUND} category, though others may
         * be maintained in the future.
         */
        public int lru;

        /**
         * The reason for {@link #importance}, if any.
         */
        public int importanceReasonCode;

        /**
         * For the specified values of {@link #importanceReasonCode}, this
         * is the process ID of the other process that is a client of this
         * process.  This will be 0 if no other process is using this one.
         */
        public int importanceReasonPid;

        /**
         * For the specified values of {@link #importanceReasonCode}, this
         * is the name of the component that is being used in this process.
         */
        public ComponentName importanceReasonComponent;

        /**
         * When {@link #importanceReasonPid} is non-0, this is the importance
         * of the other pid. @hide
         */
        public int importanceReasonImportance;

        /**
         * Current process state, as per PROCESS_STATE_* constants.
         * @hide
         */
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        public int processState;

    }

    /**
     * Information you can retrieve about tasks that the user has most recently
     * started or visited.
     */
    public static class RecentTaskInfo {

        public Intent baseIntent;

    }

    /**
     * Flag for use with {@link #getRecentTasks}: return all tasks, even those
     * that have set their
     * {@link android.content.Intent#FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS} flag.
     */
    public static final int RECENT_WITH_EXCLUDED = 0x0001;

    /**
     * Provides a list that does not contain any
     * recent tasks that currently are not available to the user.
     */
    public static final int RECENT_IGNORE_UNAVAILABLE = 0x0002;

}
