package me.piebridge.brevent.ui;

import android.app.ActionBar;
import android.app.AppOpsManager;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toolbar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.brevent.protocol.BreventOpsOK;
import me.piebridge.brevent.protocol.BreventOpsReset;
import me.piebridge.brevent.protocol.BreventOpsUpdate;
import me.piebridge.brevent.protocol.BreventProtocol;
import me.piebridge.stats.StatsUtils;

/**
 * Created by thom on 2017/10/21.
 */
public class BreventOps extends AbstractActivity {

    private int mColorControlNormal;
    private OpsFragment opsFragment;
    private Toolbar toolbar;

    private ExecutorService executor = new ScheduledThreadPoolExecutor(0x1);

    private static final int ACTION_RESET = -1;

    private static final int ACTION_ALLOW = AppOpsManager.MODE_ALLOWED;

    private static final int ACTION_IGNORE = AppOpsManager.MODE_IGNORED;

    private static final String FRAGMENT_SORT = "sort";
    private static final String FRAGMENT_PROGRESS = "progress";

    private boolean supportAppops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ops);

        toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mColorControlNormal = ColorUtils.resolveColor(this, android.R.attr.colorControlNormal);

        BreventApplication application = (BreventApplication) getApplication();
        supportAppops = application.supportAppops();
        if (supportAppops && !PreferencesUtils.getPreferences(application)
                .getBoolean(SettingsFragment.BREVENT_APPOPS, false)) {
            supportAppops = false;
        }

        if (BuildConfig.RELEASE) {
            Map<String, Object> attributes = new ArrayMap<>();
            attributes.put("appops", supportAppops ? "true" : "false");
            attributes.put("paid", application.getDonated());
            StatsUtils.logInvite(attributes);
        }

        opsFragment = new OpsFragment();
        opsFragment.getArguments().putString(Intent.EXTRA_PACKAGE_NAME, getOpsPackage());

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.content, opsFragment)
                .commit();
    }

    String getOpsPackage() {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }
        String opsPackage = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME);
        if (TextUtils.isEmpty(opsPackage)) {
            return null;
        }
        BreventApplication application = (BreventApplication) getApplication();
        if (application.getInstantPackageInfo(opsPackage) != null) {
            return opsPackage;
        }
        try {
            getPackageManager().getApplicationInfo(opsPackage, 0);
            return opsPackage;
        } catch (PackageManager.NameNotFoundException e) {
            UILog.w("Can't find " + opsPackage, e);
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String opsPackage = getOpsPackage();
        if (TextUtils.isEmpty(opsPackage)) {
            finish();
        } else {
            updateTitle(opsPackage);
        }
    }

    private void updateTitle(String opsPackage) {
        BreventApplication application = (BreventApplication) getApplication();
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = application.getInstantPackageInfo(opsPackage);
            if (packageInfo == null) {
                packageInfo = packageManager.getPackageInfo(opsPackage, 0);
            }
            setTitle(AppsLabelLoader.loadLabel(packageManager, packageInfo));
        } catch (PackageManager.NameNotFoundException e) {
            UILog.w("Can't find " + opsPackage, e);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (clearSelected()) {
                    finish();
                }
                return true;
            case R.id.action_allow:
                request(ACTION_ALLOW);
                return true;
            case R.id.action_ignore:
                request(ACTION_IGNORE);
                return true;
            case R.id.action_reset:
                request(ACTION_RESET);
                return true;
            case R.id.action_sort:
                showSort();
                return true;
            case R.id.action_select_all:
                selectAll();
                return true;
            case R.id.action_select_inverse:
                selectInverse();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void request(final int action) {
        showProgress();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                switch (action) {
                    case ACTION_RESET:
                        doRequest(new BreventOpsReset(getOpsPackage()));
                        break;
                    case ACTION_ALLOW:
                    case ACTION_IGNORE:
                        doRequest(new BreventOpsUpdate(action, getOpsPackage(), getOps()));
                        break;
                }
            }
        });
    }

    void refresh() {
        clearSelected();
        opsFragment.refresh();
    }

    Collection<Integer> getOps() {
        return opsFragment.getOps();
    }

    boolean doRequest(BreventProtocol request) {
        boolean result = false;
        try (
                Socket socket = new Socket(BreventProtocol.HOST, BreventProtocol.PORT)
        ) {
            socket.setSoTimeout(5000);

            DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            BreventProtocol.writeTo(request, os);
            os.flush();

            DataInputStream is = new DataInputStream(socket.getInputStream());
            BreventProtocol response = BreventProtocol.readFrom(is);

            os.close();
            is.close();
            result = response == BreventOpsOK.INSTANCE;
            UILog.i("request: " + request + ", response: " + response);
        } catch (IOException e) {
            UILog.w("Can't request " + request, e);
        }
        if (!isStopped()) {
            final boolean update = result;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isStopped()) {
                        hideProgress();
                        if (update) {
                            refresh();
                        } else {
                            ((BreventApplication) getApplication()).setGrantedWarned(false);
                            finish();
                        }
                    }
                }
            });
        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_ops, menu);
        if (!supportAppops) {
            menu.removeItem(R.id.action_allow);
            menu.removeItem(R.id.action_ignore);
            menu.removeItem(R.id.action_reset);
            menu.removeItem(R.id.action_select_all);
            menu.removeItem(R.id.action_select_inverse);
        } else if (getSelectedSize() > 0) {
            if (opsFragment.canAllow()) {
                menu.findItem(R.id.action_allow).getIcon().setTint(mColorControlNormal);
            } else {
                menu.removeItem(R.id.action_allow);
            }
            if (opsFragment.canIgnore()) {
                menu.findItem(R.id.action_ignore).getIcon().setTint(mColorControlNormal);
            } else {
                menu.removeItem(R.id.action_ignore);
            }
            menu.removeItem(R.id.action_reset);
        } else {
            menu.removeItem(R.id.action_allow);
            menu.removeItem(R.id.action_ignore);
            menu.findItem(R.id.action_reset).getIcon().setTint(mColorControlNormal);
            menu.removeItem(R.id.action_select_inverse);
        }
        menu.findItem(R.id.action_sort).getIcon().setTint(mColorControlNormal);
        return true;
    }

    public void showSort() {
        if (isStopped()) {
            return;
        }
        OpsSortFragment fragment = (OpsSortFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_SORT);
        if (fragment != null) {
            fragment.dismiss();
        }
        fragment = new OpsSortFragment();
        fragment.show(getFragmentManager(), FRAGMENT_SORT);
    }

    public void showProgress() {
        if (isStopped()) {
            return;
        }
        ProgressFragment fragment = (ProgressFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_PROGRESS);
        if (fragment != null) {
            fragment.dismiss();
        }
        fragment = new ProgressFragment();
        fragment.setMessage(R.string.process_checking);
        fragment.show(getFragmentManager(), FRAGMENT_PROGRESS);
    }

    public void hideProgress() {
        dismissDialog(FRAGMENT_PROGRESS);
    }

    private void dismissDialog(String tag) {
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(tag);
        if (fragment != null && !isStopped()) {
            fragment.dismiss();
        }
    }

    public void updateSort() {
        opsFragment.updateSort();
    }

    public void updateSelected(int size) {
        if (size > 0) {
            toolbar.setSubtitle(String.valueOf(size));
        } else {
            toolbar.setSubtitle(null);
        }
        invalidateOptionsMenu();
    }

    public int getSelectedSize() {
        return opsFragment.getSelectedSize();
    }

    @Override
    public void onBackPressed() {
        if (clearSelected()) {
            super.onBackPressed();
        }
    }

    private boolean clearSelected() {
        if (getSelectedSize() > 0) {
            opsFragment.clearSelected();
            opsFragment.updateSelected();
            return false;
        } else {
            return true;
        }
    }

    private void selectInverse() {
        opsFragment.selectInverse();
        opsFragment.updateSelected();
    }

    private void selectAll() {
        opsFragment.selectAll();
        opsFragment.updateSelected();
    }

}