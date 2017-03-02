package me.piebridge.brevent.ui;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

import me.piebridge.brevent.R;

/**
 * Created by thom on 15/10/3.
 */
public class ListPreferenceSummary extends ListPreference {

    public ListPreferenceSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        String value = getValue();
        switch (value) {
            case "0":
                return getContext().getString(R.string.brevent_timeout_label_never);
            case "later":
                return getContext().getString(R.string.brevent_mode_label_later);
            case "immediate":
                return getContext().getString(R.string.brevent_mode_label_immediate);
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
