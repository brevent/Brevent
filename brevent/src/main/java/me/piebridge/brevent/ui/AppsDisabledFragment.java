package me.piebridge.brevent.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.KeyEvent;
import android.widget.Toast;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.override.HideApiOverride;

/**
 * Created by thom on 2017/2/5.
 */
public class AppsDisabledFragment extends DialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnKeyListener {

    private static final String TITLE = "title";

    private static final String USB_CONNECTED = "USB_CONNECTED";

    private static final int DEFAULT_TITLE = R.string.brevent_service_start;

    private Dialog mDialog;

    private int repeat;

    public AppsDisabledFragment() {
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mDialog == null) {
            mDialog = createDialog();
        }
        return mDialog;
    }

    static boolean isEmulator() {
        return "1".equals(SystemProperties.get("ro.kernel.qemu", Build.UNKNOWN));
    }

    private Dialog createDialog() {
        BreventActivity activity = (BreventActivity) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(R.mipmap.ic_launcher);
        Bundle arguments = getArguments();
        builder.setTitle(getString(arguments.getInt(TITLE, DEFAULT_TITLE),
                BuildConfig.VERSION_NAME));
        boolean adbRunning = "running".equals(SystemProperties.get("init.svc.adbd", Build.UNKNOWN));
        String adbStatus = adbRunning ? getString(R.string.brevent_service_adb_running) : "";
        IntentFilter filter = new IntentFilter(HideApiOverride.ACTION_USB_STATE);
        Intent intent = activity.registerReceiver(null, filter);
        boolean usb = intent != null && intent.getBooleanExtra(HideApiOverride.USB_CONNECTED, false);
        arguments.putBoolean(USB_CONNECTED, usb);
        String commandLine = getBootstrapCommandLine();
        String usbStatus = usb ? getString(R.string.brevent_service_usb_connected) : "";
        builder.setMessage(getString(R.string.brevent_service_guide,
                adbStatus, usbStatus, commandLine));
        if (activity.canFetchLogs()) {
            builder.setNeutralButton(R.string.menu_logs, this);
        } else {
            builder.setNeutralButton(R.string.menu_guide, this);
        }
        if (allowRoot()) {
            builder.setNegativeButton(R.string.brevent_service_run_as_root, this);
        } else {
            if (usb) {
                builder.setPositiveButton(R.string.brevent_service_copy_path, this);
            } else {
                builder.setPositiveButton(R.string.brevent_service_open_development, this);
            }
        }
        builder.setOnKeyListener(this);
        return builder.create();
    }

    private boolean allowRoot() {
        return ((BreventApplication) getActivity().getApplication()).allowRoot();
    }

    public void update(int title) {
        Bundle arguments = getArguments();
        arguments.putInt(TITLE, title);
        if (mDialog != null) {
            mDialog.setTitle(title);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mDialog = null;
    }

    private String getBootstrapCommandLine() {
        BreventApplication application = (BreventApplication) getActivity().getApplication();
        String path = application.copyBrevent();
        if (path != null) {
            StringBuilder sb = new StringBuilder();
            if (isEmulator()) {
                sb.append("adb -e shell ");
            } else if (isConnected()) {
                sb.append("adb -d shell ");
            } else {
                sb.append("adb shell ");
            }
            sb.append("sh ");
            sb.append(path);
            return sb.toString();
        } else {
            return getString(R.string.unsupported_path);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        BreventActivity activity = (BreventActivity) getActivity();
        if (activity == null) {
            // do nothing
        } else if (which == DialogInterface.BUTTON_POSITIVE) {
            IntentFilter filter = new IntentFilter(HideApiOverride.ACTION_USB_STATE);
            Intent intent = getActivity().registerReceiver(null, filter);
            boolean usb = intent != null && intent.getBooleanExtra(HideApiOverride.USB_CONNECTED,
                    false);
            if (usb) {
                String commandLine = getBootstrapCommandLine();
                activity.copy(commandLine);
                String message = getString(R.string.brevent_service_command_copied, commandLine);
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            } else {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.android.settings",
                        "com.android.settings.DevelopmentSettings"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                try {
                    startActivity(intent);
                    getActivity().finish();
                } catch (ActivityNotFoundException e) {
                    UILog.d("Can't find settings", e);
                }
            }
            activity.showDisabled(getArguments().getInt(TITLE, DEFAULT_TITLE), true);
        } else if (which == DialogInterface.BUTTON_NEUTRAL) {
            if (activity.canFetchLogs()) {
                activity.fetchLogs();
            } else {
                activity.openGuide("disabled");
            }
            dismiss();
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            activity.runAsRoot();
            dismiss();
        }
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN
                && ++repeat == 0x7) {
            BreventActivity activity = (BreventActivity) getActivity();
            ((BreventApplication) activity.getApplication()).toggleAllowRoot();
            activity.showDisabled(getArguments().getInt(TITLE, DEFAULT_TITLE), true);
        }
        return false;
    }

    public int getTitle() {
        return getArguments().getInt(TITLE);
    }

    public boolean isConnected() {
        return getArguments().getBoolean(USB_CONNECTED, false);
    }

}
