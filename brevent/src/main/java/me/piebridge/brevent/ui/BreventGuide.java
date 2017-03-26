package me.piebridge.brevent.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toolbar;

import me.piebridge.brevent.R;

public class BreventGuide extends Activity {

    public static final String GUIDE = "guide";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(mToolbar);

        Resources resources = getResources();
        String[] titles = resources.getStringArray(R.array.fragment_guide);
        String[] messages = resources.getStringArray(R.array.fragment_guide_message);
        String[] messages2 = resources.getStringArray(R.array.fragment_guide_message2);
        String titleShowButton = resources.getString(R.string.fragment_guide_enjoy);

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new GuidePagerAdapter(getFragmentManager(), titles, titleShowButton, messages, messages2));
    }

    final void startBrevent() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (preferences.getBoolean(GUIDE, true)) {
            preferences.edit().putBoolean(GUIDE, false).apply();
            startActivity(new Intent(this, BreventActivity.class));
        }
        finish();
    }

    public static class GuidePagerAdapter extends FragmentPagerAdapter {

        private final String[] mTitles;
        private final String mTitleShowButton;
        private final String[] mMessages;
        private final String[] mMessages2;

        GuidePagerAdapter(FragmentManager fragmentManager, String[] titles, String titleShowbutton, String[] messages, String[] messages2) {
            super(fragmentManager);
            mTitles = titles;
            mTitleShowButton = titleShowbutton;
            mMessages = messages;
            mMessages2 = messages2;
        }

        @Override
        public Fragment getItem(int position) {
            GuideFragment fragment = new GuideFragment();
            Bundle arguments = fragment.getArguments();
            arguments.putString(GuideFragment.MESSAGE, mMessages[position]);
            arguments.putString(GuideFragment.MESSAGE2, mMessages2[position]);
            if (mTitles[position].equals(mTitleShowButton)) {
                arguments.putBoolean(GuideFragment.BUTTON, true);
            } else {
                arguments.putBoolean(GuideFragment.BUTTON, false);
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return mTitles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }

    }

    public static class GuideFragment extends Fragment implements View.OnClickListener {

        private View mView;

        static final String MESSAGE = "message";

        static final String MESSAGE2 = "message2";

        static final String BUTTON = "button";

        public GuideFragment() {
            setArguments(new Bundle());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if (mView == null) {
                mView = inflater.inflate(R.layout.fragment_guide, container, false);
                Bundle arguments = getArguments();
                ((TextView) mView.findViewById(R.id.message)).setText(arguments.getString(MESSAGE, ""));
                ((TextView) mView.findViewById(R.id.message2)).setText(arguments.getString(MESSAGE2, ""));
                View button = mView.findViewById(R.id.button);
                if (arguments.getBoolean(BUTTON)) {
                    button.setVisibility(View.VISIBLE);
                    button.setOnClickListener(this);
                } else {
                    button.setVisibility(View.GONE);
                }
            }
            return mView;
        }

        @Override
        public void onDestroy() {
            mView = null;
            super.onDestroy();
        }

        @Override
        public void onClick(View v) {
            ((BreventGuide) getActivity()).startBrevent();
        }

    }

}
