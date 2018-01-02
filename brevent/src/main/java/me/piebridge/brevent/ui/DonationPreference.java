package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * Created by thom on 17/12/30.
 */
public class DonationPreference extends SwitchPreference {

    static final String DONATION_RECOMMEND = "donation_recommend";

    private final Context mContext;

    private final Donation mDonation;

    private final int mRequire;

    private final int mRecommend;

    public DonationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mDonation = BuildConfig.RELEASE ? (Donation) context.getApplicationContext() : null;

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.me_piebridge_brevent_ui_DonationPreference);
        mRequire = a.getInt(R.styleable.me_piebridge_brevent_ui_DonationPreference_require, 0);
        mRecommend = a.getInt(R.styleable.me_piebridge_brevent_ui_DonationPreference_recommend, 0);
        a.recycle();
    }

    @Override
    public void setChecked(boolean checked) {
        if (!BuildConfig.RELEASE || mDonation == null) {
            super.setChecked(checked);
        } else if (mRequire > 0) {
            int donated = mDonation.getDonated();
            if (donated >= mRequire) {
                super.setChecked(checked);
            } else {
                setEnabled(false);
                super.setChecked(false);
            }
        } else {
            super.setChecked(checked);
            if (mRecommend > 0) {
                mDonation.setRecommend(getKey(), mRecommend, checked);
            }
        }
    }

    @Override
    public CharSequence getSummary() {
        CharSequence summary = super.getSummary();
        if (!BuildConfig.RELEASE || mDonation == null) {
            return summary;
        }
        if (mRequire > 0) {
            return append(summary, mDonation.getRequire(mContext.getResources(), mRequire));
        } else if (mRecommend > 0) {
            return append(summary, mDonation.getRecommend(mContext.getResources(), mRecommend));
        } else {
            return summary;
        }
    }

    private CharSequence append(CharSequence summary, CharSequence extra) {
        if (summary == null) {
            return extra;
        }
        if (extra == null) {
            return summary;
        }
        if (summary.toString().contains("\n\n")) {
            return summary + "\n" + extra;
        } else {
            return summary + "\n\n" + extra;
        }
    }

    public void updateDonated(int donated) {
        if (mDonation != null && mRequire > 0) {
            setEnabled(donated >= mRequire);
        }
    }

    interface Donation {

        CharSequence getRecommend(Resources resources, int recommend);

        CharSequence getRequire(Resources resources, int require);

        int getDonated();

        void setRecommend(String key, int val, boolean checked);

    }

}
