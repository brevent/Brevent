package me.piebridge.brevent.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/8/4.
 */
public class SortFragment extends AbstractDialogFragment implements DialogInterface.OnClickListener {

    private static final String SORT_METHOD = "sort_method";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.menu_sort);
        int itemsId;
        int checked = getChecked(activity);
        if (((BreventApplication) activity.getApplication()).supportStats()) {
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
        if (getChecked(activity) != which) {
            PreferenceManager.getDefaultSharedPreferences(activity)
                    .edit().putInt(SORT_METHOD, which).apply();
            activity.updateSort();
        }
        dismiss();
    }

    public static int getChecked(Context context) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(context).getInt(SORT_METHOD, 0);
        } catch (ClassCastException e) { // NOSONAR
            // do nothing
            return 0;
        }
    }

}
