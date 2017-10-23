package me.piebridge.brevent.ui;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by thom on 2017/1/25.
 */
public class OpsItemViewHolder extends RecyclerView.ViewHolder {

    int op;
    String name;
    CardView cardView;
    ImageView iconView;
    TextView modeView;
    TextView nameView;
    TextView labelView;
    TextView timeView;
    Drawable icon;

    private final OpsFragment mFragment;

    public OpsItemViewHolder(OpsFragment fragment, CardView view) {
        super(view);
        mFragment = fragment;
        view.setLongClickable(false);
    }

}
