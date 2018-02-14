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
import me.piebridge.donation.DonateActivity;

/**
 * Created by thom on 2017/2/5.
 */
public class AppsDonateFragment extends AbstractDialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnShowListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(BuildConfig.ICON);
        builder.setTitle(R.string.brevent);
        builder.setMessage(R.string.pay_verify);
        builder.setPositiveButton(R.string.pay_alipay, this);
        builder.setNegativeButton(R.string.pay_email, this);
        builder.setNeutralButton(android.R.string.ok, this);
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(this);
        return alertDialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        AbstractActivity activity = (AbstractActivity) getActivity();
        if (activity == null || activity.isStopped()) {
            return;
        }
        if (DialogInterface.BUTTON_POSITIVE == which) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.DONATE_ALIPAY));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            activity.startActivity(intent);
        } else if (DialogInterface.BUTTON_NEGATIVE == which) {
            BreventApplication application = (BreventApplication) activity.getApplication();
            String subject = getString(R.string.pay_email_subject, BuildConfig.VERSION_NAME,
                    Long.toHexString(BreventApplication.getId(application)));
            String content = getString(R.string.pay_email_text, Build.FINGERPRINT);
            BreventActivity.sendEmail(activity, subject, content);
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

}
