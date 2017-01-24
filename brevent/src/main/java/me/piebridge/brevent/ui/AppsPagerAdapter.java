package me.piebridge.brevent.ui;

import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.ViewGroup;

/**
 * Created by thom on 2017/1/25.
 */
public class AppsPagerAdapter extends FragmentPagerAdapter {

    private static final int FRAGMENT_USER_APPS = 0;

    private static final int FRAGMENT_SYSTEM_APPS = 1;

    private static final int FRAGMENT_FRAMEWORK_APPS = 2;

    private final AppsFragment[] mFragments;

    private final String[] mTitles;

    private boolean mShowFramework;

    private boolean mShowAllApps;

    public AppsPagerAdapter(FragmentManager fm, String[] titles, boolean showFramework) {
        super(fm);
        this.mFragments = new AppsFragment[titles.length];
        this.mTitles = titles;
        this.mShowFramework = showFramework;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        AppsFragment fragment = (AppsFragment) super.instantiateItem(container, position);
        mFragments[position] = fragment;
        return fragment;
    }

    @Override
    public AppsFragment getItem(int position) {
        switch (position) {
            case FRAGMENT_USER_APPS:
                return new UserAppsFragment();
            case FRAGMENT_SYSTEM_APPS:
                return new SystemAppsFragment();
            case FRAGMENT_FRAMEWORK_APPS:
                return new FrameworkAppsFragment();
            default:
                return null;
        }
    }

    public AppsFragment getFragment(int position) {
        return mFragments[position];
    }

    @Override
    public int getCount() {
        return mShowFramework ? mFragments.length : mFragments.length - 1;
    }

    @Override
    public int getItemPosition(Object object) {
        if (!mShowFramework && object == mFragments[FRAGMENT_FRAMEWORK_APPS]) {
            return POSITION_NONE;
        } else {
            return POSITION_UNCHANGED;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }

    public void setShowFramework(boolean showFramework) {
        if (mShowFramework != showFramework) {
            mShowFramework = showFramework;
        }
    }

    public void refreshFragment() {
        notifyDataSetChanged();
        for (AppsFragment fragment : mFragments) {
            if (fragment != null) {
                fragment.refresh();
            }
        }
    }

    public void setShowAllApps(boolean showAllApps) {
        if (mShowAllApps != showAllApps) {
            mShowAllApps = showAllApps;
            for (AppsFragment fragment : mFragments) {
                if (fragment != null && !fragment.supportAllApps()) {
                    fragment.setExpired();
                }
            }
        }
    }

}
