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
        implements DialogInterface.OnKeyListener {

    private static final String MESSAGE = "MESSAGE";

    public UnsupportedFragment() {
        setArguments(new Bundle());
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(getString(R.string.brevent) + " " + BuildConfig.VERSION_NAME);
        builder.setMessage(getString(getArguments().getInt(MESSAGE)));
        builder.setOnKeyListener(this);
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finishActivity();
    }

    private void finishActivity() {
        int message = getArguments().getInt(MESSAGE);
        if (message == R.string.unsupported_logs) {
            return;
        }
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
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

}
