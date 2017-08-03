package me.piebridge.brevent.ui;

import android.app.DialogFragment;
import android.os.Bundle;

/**
 * Created by thom on 2017/8/3.
 */
public abstract class AbstractDialogFragment extends DialogFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // https://stackoverflow.com/a/27084544
        if (super.getDialog() == null) {
            super.setShowsDialog(false);
        }
        super.onActivityCreated(savedInstanceState);
    }

}
