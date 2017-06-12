package android.app;

/**
 * Activity Manager
 * Created by thom on 2017/2/7.
 */
public class ActivityManager {

    /**
     * @hide Process does not exist.
     */
    // since api-23
    public static int PROCESS_STATE_NONEXISTENT = -1;

    /**
     * @hide Process is a persistent system process.
     */
    public static int PROCESS_STATE_PERSISTENT = 0;

    /**
     * @hide Process is a persistent system process and is doing UI.
     */
    public static int PROCESS_STATE_PERSISTENT_UI = 1;

    /**
     * @hide Process is hosting the current top activities.  Note that this covers
     * all activities that are visible to the user.
     */
    public static int PROCESS_STATE_TOP = 2;

    /**
     * @hide Process is hosting a foreground service due to a system binding.
     */
    // since api-23
    public static int PROCESS_STATE_BOUND_FOREGROUND_SERVICE = 3;

    /**
     * @hide Process is hosting a foreground service.
     */
    // since api-23
    public static int PROCESS_STATE_FOREGROUND_SERVICE = 4;

    /**
     * @hide Same as {@link #PROCESS_STATE_TOP} but while device is sleeping.
     */
    // since api-23
    public static int PROCESS_STATE_TOP_SLEEPING = 5;

    /**
     * @hide Process is important to the user, and something they are aware of.
     */
    public static int PROCESS_STATE_IMPORTANT_FOREGROUND = 6;

    /**
     * @hide Process is important to the user, but not something they are aware of.
     */
    public static int PROCESS_STATE_IMPORTANT_BACKGROUND = 7;

    /**
     * @hide Process is in the background running a backup/restore operation.
     */
    public static int PROCESS_STATE_BACKUP = 8;

    /**
     * @hide Process is in the background, but it can't restore its state so we want
     * to try to avoid killing it.
     */
    public static int PROCESS_STATE_HEAVY_WEIGHT = 9;

    /**
     * @hide Process is in the background running a service.  Unlike oom_adj, this level
     * is used for both the normal running in background state and the executing
     * operations state.
     */
    public static int PROCESS_STATE_SERVICE = 10;

    /**
     * @hide Process is in the background running a receiver.   Note that from the
     * perspective of oom_adj receivers run at a higher foreground level, but for our
     * prioritization here that is not necessary and putting them below services means
     * many fewer changes in some process states as they receive broadcasts.
     */
    public static int PROCESS_STATE_RECEIVER = 11;

    /**
     * @hide Process is in the background but hosts the home activity.
     */
    public static int PROCESS_STATE_HOME = 12;

    /**
     * @hide Process is in the background but hosts the last shown activity.
     */
    public static int PROCESS_STATE_LAST_ACTIVITY = 13;

    /**
     * @hide Process is being cached for later use and contains activities.
     */
    public static int PROCESS_STATE_CACHED_ACTIVITY = 14;

    /**
     * @hide Process is being cached for later use and is a client of another cached
     * process that contains activities.
     */
    public static int PROCESS_STATE_CACHED_ACTIVITY_CLIENT = 15;

    /**
     * @hide Process is being cached for later use and is empty.
     */
    public static int PROCESS_STATE_CACHED_EMPTY = 16;

    /**
     * @hide The lowest process state number
     */
    // since api-24
    public static int MIN_PROCESS_STATE = PROCESS_STATE_NONEXISTENT;

    /**
     * @hide The highest process state number
     */
    // since api-24
    public static int MAX_PROCESS_STATE = PROCESS_STATE_CACHED_EMPTY;

    /**
     * @hide Should this process state be considered a background state?
     */
    // since api-24
    public static boolean isProcStateBackground(int procState) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the userId of the current foreground user. Requires system permissions.
     *
     * @hide
     */
    public static int getCurrentUser() {
        throw new UnsupportedOperationException();
    }

    /**
     * Information you can retrieve about a running process.
     */
    public static class RunningAppProcessInfo {
        /**
         * Constant for {@link #flags}: this is an app that is unable to
         * correctly save its state when going to the background,
         * so it can not be killed while in the background.
         *
         * @hide
         */
        public static int FLAG_CANT_SAVE_STATE = 1 << 0;

        /**
         * Constant for {@link #flags}: this process is associated with a
         * persistent system app.
         *
         * @hide
         */
        public static int FLAG_PERSISTENT = 1 << 1;

        /**
         * Constant for {@link #flags}: this process is associated with a
         * persistent system app.
         *
         * @hide
         */
        public static int FLAG_HAS_ACTIVITIES = 1 << 2;

        /**
         * Flags of information.  May be any of
         * {@link #FLAG_CANT_SAVE_STATE}.
         *
         * @hide
         */
        public int flags;

        /**
         * Current process state, as per PROCESS_STATE_* constants.
         *
         * @hide
         */
        // since api-21
        public int processState;

    }

    /**
     * Information you can retrieve about tasks that the user has most recently
     * started or visited.
     */
    public static class RecentTaskInfo {
        /**
         * The id of the ActivityStack this Task was on most recently.
         *
         * @hide
         */
        public int stackId;

        /**
         * The last time this task was active.
         *
         * @hide
         */
        public long lastActiveTime;

    }

    /**
     * Provides a list that does not contain any
     * recent tasks that currently are not available to the user.
     */
    public static int RECENT_IGNORE_UNAVAILABLE = 0x0002;

    /**
     * Provides a list that contains recent tasks for all
     * profiles of a user.
     *
     * @hide
     */
    public static int RECENT_INCLUDE_PROFILES = 0x0004;

    /**
     * Ignores all tasks that are on the home stack.
     *
     * @hide
     */
    public static int RECENT_IGNORE_HOME_STACK_TASKS = 0x0008;

    // android-o
    public static int RECENT_IGNORE_HOME_AND_RECENTS_STACK_TASKS = 0x0008;

    /**
     * Ignores the top task in the docked stack.
     *
     * @hide
     */
    // since api-24
    public static int RECENT_INGORE_DOCKED_STACK_TOP_TASK = 0x0010;

    /**
     * Ignores all tasks that are on the pinned stack.
     *
     * @hide
     */
    // since api-24
    public static int RECENT_INGORE_PINNED_STACK_TASKS = 0x0020;

    /**
     * @hide
     */
    // since api-24
    public static class StackId {
        /**
         * Invalid stack ID.
         */
        public static int INVALID_STACK_ID = -1;

        /**
         * First static stack ID.
         */
        public static int FIRST_STATIC_STACK_ID = 0;

        /**
         * Home activity stack ID.
         */
        public static int HOME_STACK_ID = FIRST_STATIC_STACK_ID;

        /**
         * ID of stack where fullscreen activities are normally launched into.
         */
        public static int FULLSCREEN_WORKSPACE_STACK_ID = 1;

        /**
         * ID of stack where freeform/resized activities are normally launched into.
         */
        public static int FREEFORM_WORKSPACE_STACK_ID = FULLSCREEN_WORKSPACE_STACK_ID + 1;

        /**
         * ID of stack that occupies a dedicated region of the screen.
         */
        public static int DOCKED_STACK_ID = FREEFORM_WORKSPACE_STACK_ID + 1;

        /**
         * ID of stack that always on top (always visible) when it exist.
         */
        public static int PINNED_STACK_ID = DOCKED_STACK_ID + 1;

        /**
         * Last static stack stack ID.
         */
        public static int LAST_STATIC_STACK_ID = PINNED_STACK_ID;

        /**
         * Start of ID range used by stacks that are created dynamically.
         */
        public static int FIRST_DYNAMIC_STACK_ID = LAST_STATIC_STACK_ID + 1;
    }

}
