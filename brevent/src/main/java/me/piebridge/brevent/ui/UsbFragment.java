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
public class UsbFragment extends AbstractDialogFragment implements DialogInterface.OnClickListener {

    public UsbFragment() {
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(BuildConfig.ICON);
        builder.setTitle(getString(R.string.brevent) + " " + BuildConfig.VERSION_NAME);
        builder.setMessage(R.string.unsupported_usb);
        builder.setPositiveButton(android.R.string.ok, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // do nothing
    }

}
