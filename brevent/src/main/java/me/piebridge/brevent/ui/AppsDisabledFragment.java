package me.piebridge.brevent.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import java.io.File;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventConfiguration;
import me.piebridge.brevent.override.HideApiOverride;


/**
 * Created by thom on 2017/2/5.
 */
public class AppsDisabledFragment extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnKeyListener {

    private static final String MESSAGE = "message";

    private static final int DEFAULT_MESSAGE = R.string.brevent_service_start;

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

    private Dialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.mipmap.ic_launcher);
        Bundle arguments = getArguments();
        builder.setTitle(arguments.getInt(MESSAGE, DEFAULT_MESSAGE));
        String commandLine = getBootstrapCommandLine();
        boolean adbRunning = SystemProperties.get("init.svc.adbd", Build.UNKNOWN).equals("running");
        String adbStatus = adbRunning ? getString(R.string.brevent_service_adb_running) : "";
        IntentFilter filter = new IntentFilter(HideApiOverride.ACTION_USB_STATE);
        Intent intent = getActivity().registerReceiver(null, filter);
        boolean connected = intent != null && intent.getBooleanExtra(HideApiOverride.USB_CONNECTED, false);
        String usbStatus = connected ? getString(R.string.brevent_service_usb_connected) : "";
        builder.setMessage(getString(R.string.brevent_service_guide, adbStatus, usbStatus, commandLine));
        ((BreventActivity) getActivity()).copy(commandLine);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean allowRoot = preferences.getBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, false);
        builder.setNeutralButton(R.string.menu_guide, this);
        if (allowRoot) {
            builder.setNegativeButton(R.string.brevent_service_run_as_root, this);
        } else {
            if (!adbRunning) {
                builder.setPositiveButton(R.string.brevent_service_open_development, this);
            }
            builder.setOnKeyListener(this);
        }
        return builder.create();
    }

    public void update(int title) {
        Bundle arguments = getArguments();
        arguments.putInt(MESSAGE, title);
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
        String name = "libbrevent.so";
        try {
            PackageManager packageManager = getActivity().getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
            File file = new File(applicationInfo.nativeLibraryDir, name);
            if (file.exists()) {
                StringBuilder sb = new StringBuilder();
                sb.append("adb ");
                if (SystemProperties.get("ro.kernel.qemu", Build.UNKNOWN).equals("1")) {
                    sb.append("-e ");
                } else {
                    sb.append("-d ");
                }
                sb.append("shell ");
                sb.append(file.getAbsolutePath());
                return sb.toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        return name;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.DevelopmentSettings"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                UILog.d("Can't find settings", e);
            }
            getActivity().finish();
        } else if (which == DialogInterface.BUTTON_NEUTRAL) {
            ((BreventActivity) getActivity()).openGuide();
            dismiss();
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            ((BreventActivity) getActivity()).runAsRoot();
            dismiss();
        }
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN && ++repeat == 0x7) {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .putBoolean(BreventConfiguration.BREVENT_ALLOW_ROOT, true).apply();
            ((BreventActivity) getActivity()).showDisabled(getArguments().getInt(MESSAGE, DEFAULT_MESSAGE));
        }
        return false;
    }

}
