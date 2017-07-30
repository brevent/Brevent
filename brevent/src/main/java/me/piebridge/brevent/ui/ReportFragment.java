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
        TextView messageView = view.findViewById(R.id.message);
        messageView.setText(arguments.getInt(MESSAGE));
        TextView detailsView = view.findViewById(R.id.details);
        String details = arguments.getString(DETAILS);
        if (TextUtils.isEmpty(details)) {
            detailsView.setVisibility(View.GONE);
        } else {
            detailsView.setVisibility(View.VISIBLE);
            detailsView.setText(details);
        }
        return view;
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
        }
    }

}
