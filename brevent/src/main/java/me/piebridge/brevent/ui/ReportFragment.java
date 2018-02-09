package me.piebridge.brevent.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * for progress
 * Created by thom on 2017/2/11.
 */
public class ReportFragment extends AbstractDialogFragment {

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
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(BuildConfig.ICON);
        builder.setTitle(getString(R.string.brevent) + " " + BuildConfig.VERSION_NAME);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.fragment_report, null);
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
        builder.setView(view);
        return builder.create();
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
