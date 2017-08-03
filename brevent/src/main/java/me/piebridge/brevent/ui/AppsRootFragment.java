package me.piebridge.brevent.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/2/5.
 */
public class AppsRootFragment extends AbstractDialogFragment
        implements DialogInterface.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(R.string.brevent);
        builder.setMessage(R.string.unsupported_disable_root);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNeutralButton(R.string.root_donate_later, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        BreventActivity activity = (BreventActivity) getActivity();
        if (DialogInterface.BUTTON_POSITIVE == which) {
            activity.openSettings();
        } else if (DialogInterface.BUTTON_NEUTRAL == which) {
            activity.confirm();
            dismiss();
        }
    }

}
