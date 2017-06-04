package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.DimenRes;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;

import me.piebridge.brevent.override.HideApiOverride;

/**
 * Created by thom on 2017/6/4.
 */
public class GuideTabLayout extends TabLayout {

    private Resources mResources;

    public GuideTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Resources getResources() {
        if (mResources == null) {
            mResources = new ResourcesWrapper(super.getResources());
        }
        return mResources;
    }

    static class ResourcesWrapper extends Resources {

        Resources mWrapper;

        ResourcesWrapper(Resources wrapper) {
            super(HideApiOverride.newAssetManager(), null, null);
            mWrapper = wrapper;
        }

        @Override
        public int getDimensionPixelSize(@DimenRes int id) throws NotFoundException {
            if (id == android.support.design.R.dimen.design_tab_scrollable_min_width) {
                return (int) (Resources.getSystem().getDisplayMetrics().widthPixels / 2.5);
            } else {
                return mWrapper.getDimensionPixelSize(id);
            }
        }

    }
}
