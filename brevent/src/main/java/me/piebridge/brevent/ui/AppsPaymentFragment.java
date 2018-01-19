package me.piebridge.brevent.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/2/5.
 */
public class AppsPaymentFragment extends AbstractDialogFragment
        implements DialogInterface.OnClickListener {

    static final String DAYS = "mb_days";
    static final String SIZE = "mb_size";
    static final String REQUIRED = "mb_required";

    public AppsPaymentFragment() {
        setArguments(new Bundle());
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(BuildConfig.ICON);
        builder.setTitle(R.string.brevent);
        Bundle arguments = getArguments();
        int days = arguments.getInt(DAYS);
        int size = arguments.getInt(SIZE);
        int required = arguments.getInt(REQUIRED);
        int index = required - 1;
        String[] brefoils = getResources().getStringArray(R.array.brefoils);
        String brefoil = brefoils.length > index ? brefoils[index] : "";
        builder.setMessage(getString(R.string.make_brevent_better, days, size, brefoil));
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(android.R.string.cancel, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        BreventActivity activity = (BreventActivity) getActivity();
        if (activity == null || activity.isStopped()) {
            return;
        }
        if (DialogInterface.BUTTON_POSITIVE == which) {
            dismiss();
            activity.openSettings();
        } else if (DialogInterface.BUTTON_NEGATIVE == which) {
            dismiss();
        }
    }

    public void setMessage(int days, int size, int required) {
        Bundle arguments = getArguments();
        arguments.putInt(DAYS, days);
        arguments.putInt(SIZE, size);
        arguments.putInt(REQUIRED, required);
    }

}
