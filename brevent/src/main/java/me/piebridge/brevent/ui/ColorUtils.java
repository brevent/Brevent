package me.piebridge.brevent.ui;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.widget.Toolbar;

/**
 * Created by thom on 2017/5/16.
 */
public class ColorUtils {

    private ColorUtils() {

    }

    public static int resolveColor(Context context, int resId) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(resId, tv, true);
        if (tv.type == TypedValue.TYPE_NULL) {
            return Color.BLACK;
        } else if (isColor(tv.type)) {
            return tv.data;
        } else {
            return ContextCompat.getColor(context, tv.resourceId);
        }
    }

    private static boolean isColor(int type) {
        return type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT;
    }

    public static void fixToolbar(Context context, Toolbar toolbar) {
        toolbar.setTitleTextColor(resolveColor(context, android.R.attr.textColorPrimary));
    }

}
