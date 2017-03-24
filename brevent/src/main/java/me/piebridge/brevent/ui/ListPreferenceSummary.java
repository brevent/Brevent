package me.piebridge.brevent.ui;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventUtils;

/**
 * Created by thom on 15/10/3.
 */
public class ListPreferenceSummary extends ListPreference {

    public ListPreferenceSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!BreventUtils.supportStandby()) {
            setEntries(R.array.brevent_method_entries);
            setEntryValues(R.array.brevent_method_values);
        }
    }

    @Override
    public CharSequence getSummary() {
        String value = getValue();
        switch (value) {
            case "standby":
            case "standby_forcestop":
                return getContext().getString(R.string.brevent_method_standby_forcestop_label);
            case "standby_only":
                return getContext().getString(R.string.brevent_method_standby_only_label);
            case "forcestop_only":
                return getContext().getString(R.string.brevent_method_forcestop_only_label);
            default:
                break;
        }
        return super.getSummary();
    }

}
