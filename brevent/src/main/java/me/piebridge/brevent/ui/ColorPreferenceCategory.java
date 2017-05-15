package me.piebridge.brevent.ui;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class ColorPreferenceCategory extends PreferenceCategory {

    private final int colorAccent;

    public ColorPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        colorAccent = ColorUtils.resolveColor(context, android.R.attr.colorAccent);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        ((TextView) view.findViewById(android.R.id.title)).setTextColor(colorAccent);
    }

}
