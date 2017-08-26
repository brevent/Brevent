package me.piebridge.brevent.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/2/5.
 */
public class AppsFeedbackFragment extends AbstractDialogFragment
        implements DialogInterface.OnClickListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(BuildConfig.ICON);
        builder.setTitle(R.string.menu_feedback);
        boolean hasEmailClient = BreventActivity.hasEmailClient(activity);
        builder.setMessage(getString(R.string.feedback_message,
                hasEmailClient ? getString(R.string.feedback_message_email) : ""));
        builder.setPositiveButton(R.string.feedback_github, this);
        if (hasEmailClient) {
            builder.setNegativeButton(R.string.feedback_email, this);
        }
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (DialogInterface.BUTTON_POSITIVE == which) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.FEEDBACK));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            activity.startActivity(intent);
        } else if (DialogInterface.BUTTON_NEGATIVE == which) {
            BreventActivity.sendEmail(activity, null, Build.FINGERPRINT);
        }
    }

}
