package me.piebridge.brevent.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/8/4.
 */
public class OpsSortFragment extends AbstractDialogFragment
        implements DialogInterface.OnClickListener {

    private static final String OPS_SORT_METHOD = "ops_sort_method";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BreventOps activity = (BreventOps) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.menu_sort);
        builder.setSingleChoiceItems(R.array.ops_sort_method, getChecked(activity), this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        BreventOps activity = (BreventOps) getActivity();
        if (activity == null || activity.isStopped()) {
            return;
        }
        if (getChecked(activity) != which) {
            PreferencesUtils.getPreferences(activity)
                    .edit().putInt(OPS_SORT_METHOD, which).apply();
            activity.updateSort();
        }
        dismiss();
    }

    public static int getChecked(Activity activity) {
        try {
            return PreferencesUtils.getPreferences(activity).getInt(OPS_SORT_METHOD, 0);
        } catch (ClassCastException e) { // NOSONAR
            // do nothing
            return 0;
        }
    }

}
