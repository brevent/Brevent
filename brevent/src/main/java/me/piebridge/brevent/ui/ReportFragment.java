package me.piebridge.brevent.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import me.piebridge.brevent.R;

/**
 * for progress
 * Created by thom on 2017/2/11.
 */
public class ReportFragment extends DialogFragment {

    private static final String MESSAGE = "message";

    private static final String DETAILS = "details";

    private TextView mMessage;

    private TextView mDetails;

    public ReportFragment() {
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container);
        Bundle arguments = getArguments();
        mMessage = view.findViewById(R.id.message);
        mMessage.setText(arguments.getInt(MESSAGE));
        mDetails = view.findViewById(R.id.details);
        String details = arguments.getString(DETAILS);
        if (TextUtils.isEmpty(details)) {
            mDetails.setVisibility(View.GONE);
        } else {
            mDetails.setVisibility(View.VISIBLE);
            mDetails.setText(details);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMessage = null;
        mDetails = null;
    }

    public void setDetails(int message, String details) {
        Bundle arguments = getArguments();
        arguments.putInt(MESSAGE, message);
        arguments.putString(DETAILS, details);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        } else {
            super.onCancel(dialog);
        }
    }

}
