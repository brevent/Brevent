package android.support.v7.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;

/**
 * Created by thom on 2017/6/4.
 */
public class TintTypedArray {

    private final Context mContext;
    private final TypedArray mWrapped;

    public static TintTypedArray obtainStyledAttributes(Context context, AttributeSet set,
                                                        int[] attrs) {
        return new TintTypedArray(context, context.obtainStyledAttributes(set, attrs));
    }

    private TintTypedArray(Context context, TypedArray array) {
        mContext = context;
        mWrapped = array;
    }

    public CharSequence getText(int index) {
        return mWrapped.getText(index);
    }

    public Drawable getDrawable(int index) {
        if (mWrapped.hasValue(index)) {
            final int resourceId = mWrapped.getResourceId(index, 0);
            if (resourceId != 0) {
                return AppCompatResources.getDrawable(mContext, resourceId);
            }
        }
        return mWrapped.getDrawable(index);
    }

    public int getResourceId(int index, int defValue) {
        return mWrapped.getResourceId(index, defValue);
    }

    public void recycle() {
        mWrapped.recycle();
    }

}
