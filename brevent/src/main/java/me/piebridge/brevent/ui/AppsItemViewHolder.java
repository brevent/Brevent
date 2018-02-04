package me.piebridge.brevent.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
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

import me.piebridge.SimpleTrim;
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
    int sdk;
    long firstInstallTime;

    private final AppsFragment mFragment;

    public AppsItemViewHolder(AppsFragment fragment, CardView view) {
        super(view);
        mFragment = fragment;
        view.setOnCreateContextMenuListener(this);
        view.setLongClickable(false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        BreventActivity activity = (BreventActivity) mFragment.getActivity();
        menu.setHeaderTitle(label);
        String target = getTarget(activity);
        if (!TextUtils.isEmpty(target)) {
            menu.add(Menu.NONE, R.string.context_menu_target, Menu.NONE, target);
        }
        menu.add(Menu.NONE, R.string.context_menu_package_name, Menu.NONE,
                activity.getString(R.string.context_menu_package_name));
        menu.add(Menu.NONE, R.string.context_menu_app_info, Menu.NONE,
                activity.getString(R.string.context_menu_app_info));
        PackageManager packageManager = mFragment.getActivity().getPackageManager();
        if (BuildConfig.APPLICATION_ID.equals(packageName)) {
            menu.add(Menu.NONE, R.string.context_menu_brevent_server_info, Menu.NONE,
                    activity.getString(R.string.context_menu_brevent_server_info));
        } else if (packageManager.getLaunchIntentForPackage(packageName) != null) {
            menu.add(Menu.NONE, R.string.context_menu_open, Menu.NONE,
                    activity.getString(R.string.context_menu_open));
        }
        if (activity.isBrevent(packageName)) {
            if (activity.isPriority(packageName)) {
                menu.add(Menu.NONE, R.string.context_menu_unset_priority, Menu.NONE,
                        activity.getString(R.string.context_menu_unset_priority));
            } else {
                menu.add(Menu.NONE, R.string.context_menu_set_priority, Menu.NONE,
                        activity.getString(R.string.context_menu_set_priority));
            }
        }
        if (activity.hasOps(packageName)) {
            menu.add(Menu.NONE, R.string.context_menu_appops, Menu.NONE,
                    activity.getString(R.string.context_menu_appops));
        }
        int size = menu.size();
        for (int i = 0; i < size; ++i) {
            menu.getItem(i).setOnMenuItemClickListener(this);
        }
        String important = activity.getLabel("", packageName);
        if (!TextUtils.isEmpty(important)) {
            menu.add(SimpleTrim.trim(important));
        }
    }

    private String getTarget(Context context) {
        try {
            int sdk = context.getPackageManager().getApplicationInfo(packageName, 0).targetSdkVersion;
            return context.getString(R.string.context_menu_target, sdk, getName(sdk));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private String getName(int sdk) {
        switch (sdk) {
            case Build.VERSION_CODES.BASE:
                return "Android 1, 2008";
            case Build.VERSION_CODES.BASE_1_1:
                return "Android 1.1, 2009";
            case Build.VERSION_CODES.CUPCAKE:
                return "Android 1.5, 2009";
            case Build.VERSION_CODES.DONUT:
                return "Android 1.6, 2009";
            case Build.VERSION_CODES.ECLAIR:
                return "Android 2.0, 2009";
            case Build.VERSION_CODES.ECLAIR_0_1:
                return "Android 2.0.1, 2009";
            case Build.VERSION_CODES.ECLAIR_MR1:
                return "Android 2.1, 2010";
            case Build.VERSION_CODES.FROYO:
                return "Android 2.2, 2010";
            case Build.VERSION_CODES.GINGERBREAD:
                return "Android 2.3, 2010";
            case Build.VERSION_CODES.GINGERBREAD_MR1:
                return "Android 2.3.3, 2011";
            case Build.VERSION_CODES.HONEYCOMB:
                return "Android 3.0, 2011";
            case Build.VERSION_CODES.HONEYCOMB_MR1:
                return "Android 3.1, 2011";
            case Build.VERSION_CODES.HONEYCOMB_MR2:
                return "Android 3.2, 2011";
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH:
                return "Android 4.0, 2011";
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
                return "Android 4.0.3, 2011";
            case Build.VERSION_CODES.JELLY_BEAN:
                return "Android 4.1, 2012";
            case Build.VERSION_CODES.JELLY_BEAN_MR1:
                return "Android 4.2, 2012";
            case Build.VERSION_CODES.JELLY_BEAN_MR2:
                return "Android 4.3, 2013";
            case Build.VERSION_CODES.KITKAT:
                return "Android 4.4, 2013";
            case Build.VERSION_CODES.KITKAT_WATCH:
                return "Android 4.4W, 2014";
            case Build.VERSION_CODES.LOLLIPOP:
                return "Android 5.0, 2014";
            case Build.VERSION_CODES.LOLLIPOP_MR1:
                return "Android 5.1, 2015";
            case Build.VERSION_CODES.M:
                return "Android 6, 2015";
            case Build.VERSION_CODES.N:
                return "Android 7.0, 2016";
            case Build.VERSION_CODES.N_MR1:
                return "Android 7.1, 2016";
            case Build.VERSION_CODES.O:
                return "Android 8.0, 2017";
            case 27:
                return "Android 8.1, 2017";
            default:
                return "Unknown";
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
            case R.string.context_menu_appops:
                Intent i = new Intent(activity, BreventOps.class);
                i.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName);
                activity.startActivity(i);
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
