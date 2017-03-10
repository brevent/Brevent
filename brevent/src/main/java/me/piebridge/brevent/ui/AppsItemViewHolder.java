package me.piebridge.brevent.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/1/25.
 */
public class AppsItemViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {

    private static final String PACKAGE_SHELL = "com.android.shell";

    String packageName;
    String label;
    CardView cardView;
    ImageView iconView;
    TextView nameView;
    ImageView statusView;
    TextView descriptionView;
    TextView inactiveView;
    Drawable icon;
    int inactive;
    @DrawableRes
    int statusIconRes;

    private final AppsFragment mFragment;

    public AppsItemViewHolder(AppsFragment fragment, CardView view) {
        super(view);
        mFragment = fragment;
        view.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(nameView.getText());
        menu.add(Menu.NONE, R.string.context_menu_choose, Menu.NONE, R.string.context_menu_choose);
        menu.add(Menu.NONE, R.string.context_menu_package_name, Menu.NONE, R.string.context_menu_package_name);
        menu.add(Menu.NONE, R.string.context_menu_app_info, Menu.NONE, R.string.context_menu_app_info);
        if (BuildConfig.APPLICATION_ID.equals(packageName)) {
            menu.add(Menu.NONE, R.string.context_menu_brevent_server_info, Menu.NONE, R.string.context_menu_brevent_server_info);
        } else if (mFragment.getActivity().getPackageManager().getLaunchIntentForPackage(packageName) != null) {
            menu.add(Menu.NONE, R.string.context_menu_open, Menu.NONE, R.string.context_menu_open);
        }
        menu.add(Menu.NONE, R.string.context_menu_notifications, Menu.NONE, R.string.context_menu_notifications);
        int size = menu.size();
        for (int i = 0; i < size; ++i) {
            menu.getItem(i).setOnMenuItemClickListener(this);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.context_menu_app_info:
                openAppInfo(packageName);
                break;
            case R.string.context_menu_brevent_server_info:
                openAppInfo(PACKAGE_SHELL);
                break;
            case R.string.context_menu_choose:
                mFragment.select(Collections.singletonList(packageName));
                break;
            case R.string.context_menu_package_name:
                copy(packageName);
                break;
            case R.string.context_menu_open:
                Activity activity = mFragment.getActivity();
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    activity.startActivity(intent);
                }
                break;
            case R.string.context_menu_notifications:
                openNotifications();
                break;
            default:
                break;
        }
        return true;
    }

    private boolean openNotifications() {
        ApplicationInfo info;
        Activity activity = mFragment.getActivity();
        try {
            info = activity.getPackageManager().getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            UILog.d("cannot find package " + packageName, e);
            return false;
        }
        int uid = info.uid;
        Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("app_package", packageName)
                .putExtra("app_uid", uid);
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            UILog.d("cannot start notification for " + packageName, e);
            return false;
        }
    }

    private void copy(String packageName) {
        BreventActivity breventActivity = (BreventActivity) mFragment.getActivity();
        breventActivity.copy(packageName);
        String message = breventActivity.getString(R.string.context_menu_snackbar_copied, packageName);
        breventActivity.showSnackbar(message);
    }

    private void openAppInfo(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null));
        mFragment.startActivity(intent);
    }

}
