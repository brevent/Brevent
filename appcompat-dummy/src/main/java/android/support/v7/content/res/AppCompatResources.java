package android.support.v7.content.res;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by thom on 2017/6/4.
 */
public class AppCompatResources {

    @Nullable
    public static Drawable getDrawable(@NonNull Context context, @DrawableRes int resId) {
        return context.getDrawable(resId);
    }

}
