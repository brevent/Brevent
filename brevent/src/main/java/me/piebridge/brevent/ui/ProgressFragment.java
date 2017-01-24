package me.piebridge.brevent.ui;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import me.piebridge.brevent.R;

/**
 * for progress
 * Created by thom on 2017/2/11.
 */
public class ProgressFragment extends DialogFragment {

    private static final String MESSAGE = "message";

    private TextView mMessage;

    public ProgressFragment() {
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container);
        mMessage = (TextView) view.findViewById(R.id.message);
        mMessage.setText(getArguments().getInt(MESSAGE));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMessage = null;
    }

    private void updateMessage() {
    }

    public void updateMessage(int message) {
        Bundle arguments = getArguments();
        if (message != arguments.getInt(MESSAGE)) {
            arguments.putInt(MESSAGE, message);
            if (mMessage != null) {
                mMessage.setText(getArguments().getInt(MESSAGE));
            }
        }
    }

}
