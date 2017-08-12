package me.piebridge.brevent.ui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/1/25.
 */
public class AppsItemViewHolder extends RecyclerView.ViewHolder
        implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {

    private static final String PACKAGE_SHELL = "com.android.shell";

    String packageName;
    String label;
    CardView cardView;
    ImageView iconView;
    TextView nameView;
    ImageView syncView;
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
        view.setLongClickable(false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(label);
        menu.add(Menu.NONE, R.string.context_menu_select, Menu.NONE, R.string.context_menu_select);
        menu.add(Menu.NONE, R.string.context_menu_package_name, Menu.NONE,
                R.string.context_menu_package_name);
        menu.add(Menu.NONE, R.string.context_menu_app_info, Menu.NONE,
                R.string.context_menu_app_info);
        if (BuildConfig.APPLICATION_ID.equals(packageName)) {
            menu.add(Menu.NONE, R.string.context_menu_brevent_server_info, Menu.NONE,
                    R.string.context_menu_brevent_server_info);
        } else if (
                mFragment.getActivity().getPackageManager().getLaunchIntentForPackage(packageName) !=
                        null) {
            menu.add(Menu.NONE, R.string.context_menu_open, Menu.NONE, R.string.context_menu_open);
        }
        BreventActivity activity = (BreventActivity) mFragment.getActivity();
        if (activity.isBrevent(packageName)) {
            if (activity.isPriority(packageName)) {
                menu.add(Menu.NONE, R.string.context_menu_unset_priority, Menu.NONE,
                        R.string.context_menu_unset_priority);
            } else {
                menu.add(Menu.NONE, R.string.context_menu_set_priority, Menu.NONE,
                        R.string.context_menu_set_priority);
            }
        }
        int size = menu.size();
        for (int i = 0; i < size; ++i) {
            menu.getItem(i).setOnMenuItemClickListener(this);
        }
        String important = activity.getLabel("", packageName);
        if (!TextUtils.isEmpty(important)) {
            menu.add(AppsLabelLoader.trim(important));
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        BreventActivity activity = (BreventActivity) mFragment.getActivity();
        switch (item.getItemId()) {
            case R.string.context_menu_app_info:
                openAppInfo(packageName);
                break;
            case R.string.context_menu_brevent_server_info:
                openAppInfo(PACKAGE_SHELL);
                break;
            case R.string.context_menu_select:
                iconView.performClick();
                break;
            case R.string.context_menu_package_name:
                copy(packageName);
                break;
            case R.string.context_menu_open:
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    activity.startActivity(intent);
                }
                break;
            case R.string.context_menu_set_priority:
                activity.updatePriority(packageName, true);
                break;
            case R.string.context_menu_unset_priority:
                activity.updatePriority(packageName, false);
                break;
            default:
                break;
        }
        return true;
    }

    private void copy(String packageName) {
        BreventActivity activity = (BreventActivity) mFragment.getActivity();
        activity.copy(packageName);
        activity.showMessage(activity.getString(R.string.context_menu_message_copied, packageName));
    }

    private void openAppInfo(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        mFragment.startActivity(intent);
    }

}
