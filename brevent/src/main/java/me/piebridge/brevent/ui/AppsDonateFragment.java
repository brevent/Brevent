package me.piebridge.brevent.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.donation.DonateActivity;

/**
 * Created by thom on 2017/2/5.
 */
public class AppsDonateFragment extends DialogFragment implements DialogInterface.OnClickListener,
        DialogInterface.OnShowListener {

    private static final String MESSAGE = "MESSAGE";

    public AppsDonateFragment() {
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(R.string.brevent);
        builder.setMessage(getArguments().getInt(MESSAGE));
        builder.setPositiveButton(R.string.root_donate_alipay, this);
        builder.setNegativeButton(R.string.root_donate_email, this);
        builder.setNeutralButton(android.R.string.ok, this);
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(this);
        return alertDialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (DialogInterface.BUTTON_POSITIVE == which) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.DONATE_ALIPAY));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            activity.startActivity(intent);
        } else if (DialogInterface.BUTTON_NEGATIVE == which) {
            BreventApplication application = (BreventApplication) activity.getApplication();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.root_donate_email_subject,
                    Long.toHexString(application.getId())));
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.root_donate_email_text));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {BuildConfig.EMAIL});
            BreventActivity.sendEmail(activity, intent);
        } else if (DialogInterface.BUTTON_NEUTRAL == which) {
            dismiss();
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        BreventApplication application = (BreventApplication) activity.getApplication();
        if (application.isUnsafe() || application.getPackageManager()
                .getLaunchIntentForPackage(DonateActivity.PACKAGE_ALIPAY) == null) {
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
        if (!BreventActivity.hasEmailClient(application)) {
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
        }
    }

    public void setRoot(boolean root) {
        getArguments().putInt(MESSAGE,
                root ? R.string.root_donate_required : R.string.root_donate_verify);
    }

}
