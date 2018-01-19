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
public class SortFragment extends AbstractDialogFragment implements DialogInterface.OnClickListener {

    private static final String SORT_METHOD = "sort_method";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BreventActivity activity = (BreventActivity) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.menu_sort);
        int itemsId;
        int checked = getChecked(activity);
        if (activity.hasStats()) {
            itemsId = R.array.sort_method;
        } else {
            itemsId = R.array.sort_method_no_stats;
            if (checked > 1) {
                checked = 0;
            }
        }
        builder.setSingleChoiceItems(itemsId, checked, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        BreventActivity activity = (BreventActivity) getActivity();
        if (activity == null || activity.isStopped()) {
            return;
        }
        if (getChecked(activity) != which) {
            PreferencesUtils.getPreferences(activity)
                    .edit().putInt(SORT_METHOD, which).apply();
            activity.updateSort();
        }
        dismiss();
    }

    public static int getChecked(Activity activity) {
        try {
            return PreferencesUtils.getPreferences(activity).getInt(SORT_METHOD, 0);
        } catch (ClassCastException e) { // NOSONAR
            // do nothing
            return 0;
        }
    }

}
