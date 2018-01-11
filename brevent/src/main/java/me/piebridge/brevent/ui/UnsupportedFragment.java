package me.piebridge.brevent.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/4/7.
 */
public class UnsupportedFragment extends AbstractDialogFragment
        implements DialogInterface.OnKeyListener, DialogInterface.OnClickListener {

    private static final String MESSAGE = "MESSAGE";

    private static final String EXIT = "exit";

    public UnsupportedFragment() {
        setArguments(new Bundle());
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(BuildConfig.ICON);
        builder.setTitle(getString(R.string.brevent) + " " + BuildConfig.VERSION_NAME);
        Bundle arguments = getArguments();
        builder.setMessage(getString(arguments.getInt(MESSAGE)));
        if (!arguments.getBoolean(EXIT)) {
            builder.setPositiveButton(android.R.string.ok, this);
        }
        builder.setOnKeyListener(this);
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finishActivity();
    }

    private void finishActivity() {
        if (getArguments().getBoolean(EXIT)) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
                System.exit(0);
            }
        }
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            finishActivity();
        }
        return false;
    }

    public void setMessage(int resId) {
        getArguments().putInt(MESSAGE, resId);
    }

    public void setExit(boolean exit) {
        getArguments().putBoolean(EXIT, exit);
    }

    public int getMessage() {
        return getArguments().getInt(MESSAGE);
    }

    public boolean getExit() {
        return getArguments().getBoolean(EXIT);
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        finishActivity();
    }

}
