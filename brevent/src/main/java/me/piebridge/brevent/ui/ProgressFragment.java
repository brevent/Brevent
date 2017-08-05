package me.piebridge.brevent.ui;

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
public class ProgressFragment extends AbstractDialogFragment {

    private static final String MESSAGE = "message";

    public ProgressFragment() {
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container);
        TextView messageView = view.findViewById(R.id.message);
        messageView.setText(getArguments().getInt(MESSAGE));
        return view;
    }

    public void setMessage(int message) {
        getArguments().putInt(MESSAGE, message);
    }

    public int getMessage() {
        return getArguments().getInt(MESSAGE);
    }

}
