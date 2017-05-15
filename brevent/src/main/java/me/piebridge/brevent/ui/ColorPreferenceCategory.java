package me.piebridge.brevent.ui;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public class ColorPreferenceCategory extends PreferenceCategory {

    private final int colorAccent;

    public ColorPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        colorAccent = resolveColor(context, android.R.attr.colorAccent);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        ((TextView) view.findViewById(android.R.id.title)).setTextColor(colorAccent);
    }

    private static int resolveColor(Context context, int resId) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(resId, tv, true);
        if (isColor(tv.type)) {
            return tv.data;
        } else {
            return ContextCompat.getColor(context, tv.resourceId);
        }
    }

    private static boolean isColor(int type) {
        return type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT;
    }

}
