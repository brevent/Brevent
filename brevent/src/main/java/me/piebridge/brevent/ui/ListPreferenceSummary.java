package me.piebridge.brevent.ui;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Created by thom on 15/10/3.
 */
public class ListPreferenceSummary extends ListPreference {

    public ListPreferenceSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        CharSequence entry = getEntry();
        if (getEntries()[0].equals(entry)) {
            return entry;
        }
        return super.getSummary();
    }

}
