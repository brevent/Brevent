package me.piebridge.brevent.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * Created by thom on 2018/1/4.
 */
public class WarningFragment extends AbstractDialogFragment
        implements DialogInterface.OnClickListener {

    private static final String MESSAGE = "message";

    public WarningFragment() {
        setArguments(new Bundle());
        setCancelable(false);
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(BuildConfig.ICON);
        builder.setTitle(getString(R.string.brevent) + " " + BuildConfig.VERSION_NAME);
        builder.setMessage(getMessage());
        builder.setPositiveButton(android.R.string.ok, this);
        if (shouldShowCancel()) {
            builder.setNegativeButton(android.R.string.cancel, this);
        }
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        AbstractActivity activity = (AbstractActivity) getActivity();
        if (activity == null || activity.isStopped()) {
            return;
        }
        if (which == DialogInterface.BUTTON_POSITIVE) {
            onClickOk();
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            onClickCancel();
        }
    }

    private boolean shouldShowCancel() {
        int message = getMessage();
        switch (message) {
            case R.string.unsupported_granted:
            case R.string.unsupported_checking:
                return true;
            default:
                return false;
        }
    }

    private void onClickOk() {
        int message = getMessage();
        switch (message) {
            case R.string.unsupported_granted:
                ((BreventApplication) getActivity().getApplication()).launchDevelopmentSettings();
                break;
            case R.string.unsupported_checking:
                ((BreventActivity) getActivity()).onUnsupportedChecking();
                break;
            default:
                break;
        }
    }

    private void onClickCancel() {
        int message = getMessage();
        switch (message) {
            case R.string.unsupported_granted:
                ((BreventApplication) getActivity().getApplication()).setGrantedWarned(true);
                break;
            default:
                break;
        }
    }

    public void setMessage(int resId) {
        getArguments().putInt(MESSAGE, resId);
    }

    public int getMessage() {
        return getArguments().getInt(MESSAGE);
    }

}
