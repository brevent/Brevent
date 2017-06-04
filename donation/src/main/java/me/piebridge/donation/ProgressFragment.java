package me.piebridge.donation;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * donation progress
 * <p>
 * Created by thom on 2017/2/13.
 */
public class ProgressFragment extends DialogFragment {

    public ProgressFragment() {
        super();
        setCancelable(false);
        setStyle(STYLE_NO_TITLE, 0);
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donation_progress, container);
    }

}