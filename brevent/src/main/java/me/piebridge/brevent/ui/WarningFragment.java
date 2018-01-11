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
public class WarningFragment extends AbstractDialogFragment implements DialogInterface.OnClickListener {

    private static final String MESSAGE = "message";

    public WarningFragment() {
        setArguments(new Bundle());
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(BuildConfig.ICON);
        builder.setTitle(getString(R.string.brevent) + " " + BuildConfig.VERSION_NAME);
        builder.setMessage(getArguments().getInt(MESSAGE));
        builder.setPositiveButton(android.R.string.ok, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // do nothing
    }

    public void setMessage(int resId) {
        getArguments().putInt(MESSAGE, resId);
    }

    public int getMessage() {
        return getArguments().getInt(MESSAGE);
    }

}
