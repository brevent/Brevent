package me.piebridge.brevent.server;

import android.support.v4.util.SimpleArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EventTag we care
 * <p>
 * Created by thom on 2017/1/17.
 */
public class EventTag {

    public static final int AM_FOCUSED_ACTIVITY = 1;

    public static final int AM_NEW_INTENT = 2;

    public static final int AM_PAUSE_ACTIVITY = 3;

    public static final int AM_PROC_START = 4;

    public static final int AM_SWITCH_USER = 5;

    public static final int NOTIFICATION_CANCEL_ALL = 6;

    public static final int POWER_SCREEN_STATE = 7;

    public static final int TAG_ANSWER = 42;

    private static final int EVENT_COUNT = 7;

    private static final String TAGS_FILE = "/system/etc/event-log-tags";

    private static final Pattern PATTERN_TAG_WITH_DESC = Pattern.compile(
            "^(\\d+)\\s+([A-Za-z0-9_]+)\\s*(.*)\\s*$"); //$NON-NLS-1$
    private static final Pattern PATTERN_DESCRIPTION = Pattern.compile(
            "\\(([A-Za-z0-9_\\s]+)\\|(\\d+)(\\|\\d+)?\\)"); //$NON-NLS-1$

    private final SparseArray<String[]> mDescriptions = new SparseArray<>();

    /**
     * tag to event
     */
    private final SparseIntArray mEvents = new SparseIntArray();

    /**
     * event to tag
     */
    private final SparseIntArray mTags = new SparseIntArray();

    EventTag() throws IOException {
        readTagsFile(TAGS_FILE);
    }

    EventTag(String tagsFile) throws IOException {
        readTagsFile(tagsFile);
    }

    private void readTagsFile(String tagsFile) throws IOException {
        try (
                BufferedReader reader = new BufferedReader(new FileReader(tagsFile))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                processTag(line);
            }
        }
        if (mTags.size() < EVENT_COUNT) {
            ServerLog.w("EventTag wanted " + EVENT_COUNT + ", found " + mTags.size());
        }
    }

    private void processTag(String line) {
        if (!StringUtils.isBlank(line) && line.charAt(0) != '#') {
            Matcher m = PATTERN_TAG_WITH_DESC.matcher(line);
            if (m.matches()) {
                int code = Integer.parseInt(m.group(1));
                String name = m.group(2);
                int event;
                if (!StringUtils.isBlank(name) && (event = parseEvent(name)) != -1) {
                    if (Log.isLoggable(ServerLog.TAG, Log.DEBUG)) {
                        ServerLog.d(name + ", tag: " + code + ", event: " + event);
                    }
                    mEvents.put(code, event);
                    mTags.put(event, code);
                    String description = m.group(3);
                    if (!StringUtils.isBlank(description)) {
                        mDescriptions.put(code, processDescription(description));
                    }
                }
            }
        }
    }

    private int parseEvent(String name) {
        switch (name) {
            case "am_focused_activity":
                return AM_FOCUSED_ACTIVITY;
            case "am_new_intent":
                return AM_NEW_INTENT;
            case "am_pause_activity":
                return AM_PAUSE_ACTIVITY;
            case "am_proc_start":
                return AM_PROC_START;
            case "am_switch_user":
                return AM_SWITCH_USER;
            case "notification_cancel_all":
                return NOTIFICATION_CANCEL_ALL;
            case "power_screen_state":
                return POWER_SCREEN_STATE;
            default:
                return -1;
        }
    }

    public static String getEventName(int event) {
        switch (event) {
            case AM_FOCUSED_ACTIVITY:
                return "am_focused_activity";
            case AM_NEW_INTENT:
                return "am_new_intent";
            case AM_PAUSE_ACTIVITY:
                return "am_pause_activity";
            case AM_PROC_START:
                return "am_proc_start";
            case AM_SWITCH_USER:
                return "am_switch_user";
            case NOTIFICATION_CANCEL_ALL:
                return "notification_cancel_all";
            case POWER_SCREEN_STATE:
                return "power_screen_state";
            default:
                return "(Unknown)";
        }
    }

    private String[] processDescription(String description) {
        String[] descriptions = description.split("\\s*,\\s*"); //$NON-NLS-1$
        String[] names = new String[descriptions.length];
        int index = 0;
        for (String desc : descriptions) {
            Matcher m = PATTERN_DESCRIPTION.matcher(desc);
            if (m.matches()) {
                String name = m.group(1);
                names[index] = formatName(name);
            }
            index++;
        }
        return names;
    }

    private String formatName(String name) {
        return toTitleCase(name);
    }

    public boolean contains(int tag) {
        return mEvents.indexOfKey(tag) >= 0;
    }

    private static String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = false;
        boolean nextLowerCase = true;
        for (char c : input.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                nextTitleCase = true;
                continue;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
                nextLowerCase = true;
            } else if (nextLowerCase) {
                if (Character.isUpperCase(c)) {
                    c = Character.toLowerCase(c);
                } else {
                    nextLowerCase = false;
                }
            }
            titleCase.append(c);
        }
        return titleCase.toString();
    }

    public SimpleArrayMap<String, Object> buildEvents(int tag, Object[] events) {
        String[] names = mDescriptions.get(tag);
        if (names == null) {
            return new SimpleArrayMap<>();
        }
        if (names.length != events.length) {
            ServerLog.e("names: " + Arrays.toString(names) + ", events: " + Arrays.toString(events));
        }
        int size = Math.min(names.length, events.length);
        SimpleArrayMap<String, Object> result = new SimpleArrayMap<>();
        for (int i = 0; i < size; ++i) {
            try {
                result.put(names[i], events[i]);
            } catch (ArrayIndexOutOfBoundsException e) {
                ServerLog.e("names: " + Arrays.toString(names) + ", events: " + Arrays.toString(events) + ", i: " + i + ", size: " + size, e);
            }
        }
        return result;
    }

    public int getEvent(int tag) {
        return mEvents.get(tag);
    }

    public int getTag(int event) {
        return mTags.get(event);
    }

}
