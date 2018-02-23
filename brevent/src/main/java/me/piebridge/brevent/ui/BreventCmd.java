package me.piebridge.brevent.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toolbar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.Objects;

import me.piebridge.SimpleTrim;
import me.piebridge.brevent.R;
import me.piebridge.brevent.override.HideApiOverride;
import me.piebridge.brevent.protocol.BreventCmdRequest;
import me.piebridge.brevent.protocol.BreventCmdResponse;
import me.piebridge.brevent.protocol.BreventIntent;
import me.piebridge.brevent.protocol.BreventProtocol;

/**
 * Created by thom on 2018/1/27.
 */
public class BreventCmd extends AbstractActivity implements View.OnClickListener {

    private static final long LATER = 400;
    private static final int MAX_SIZE = 100 * 1024;

    private int mColorControlNormal;

    private EditText commandView;
    private ImageView execView;
    private TextView outputView;

    private Handler workHandler;

    private static final int REQUEST = 0;

    private static final int RESPONSE = 1;
    private static final int PROTOCOL = 2;
    private static final int EXCEPTION = 3;
    private static final int OUTPUT = 4;

    private int outputSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cmd);

        mColorControlNormal = ColorUtils.resolveColor(this, android.R.attr.colorControlNormal);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);

        if (isActionCommand()) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_code_black_24dp, getTheme());
            drawable.setTint(mColorControlNormal);
            toolbar.setNavigationIcon(drawable);
        } else if (isActionDeveloper()) {
            ((BreventApplication) getApplication()).launchDevelopmentSettings();
            finish();
        } else {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        commandView = findViewById(R.id.command);
        execView = findViewById(R.id.exec);
        outputView = findViewById(R.id.output);

        execView.setOnClickListener(this);

        workHandler = new WorkHandler(this, new MainHandler(this));

        BreventApplication application = (BreventApplication) getApplication();
        if (application.getDonated() < BreventSettings.CONTRIBUTOR) {
            WarningFragment fragment = new WarningFragment();
            fragment.setMessage(R.string.cmd_warning);
            fragment.show(getFragmentManager(), "command");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(R.string.menu_command);
    }

    @Override
    protected void onDestroy() {
        if (workHandler != null) {
            Looper looper = workHandler.getLooper();
            if (looper != null) {
                looper.quit();
            }
            workHandler = null;
        }
        super.onDestroy();
    }

    private boolean isActionCommand() {
        Intent intent = getIntent();
        return intent != null && BreventIntent.ACTION_COMMAND.equals(intent.getAction());
    }

    private boolean isActionDeveloper() {
        Intent intent = getIntent();
        return intent != null && BreventIntent.ACTION_DEVELOPER.equals(intent.getAction());
    }

    @Override
    public void onClick(View v) {
        if (v == execView) {
            String command = commandView.getText().toString();
            String safeCommand = SimpleTrim.removeWhiteSpace(command);
            if (!Objects.equals(command, safeCommand)) {
                command = safeCommand;
                commandView.setText(command);
                commandView.setSelection(command.length());
            }
            if (TextUtils.isEmpty(command)) {
                reset(false);
            } else if (isInvalid(command.split("[;|\n]"))) {
                reset(true);
            } else {
                commandView.setEnabled(false);
                execView.setClickable(false);
                execView.setImageResource(android.R.drawable.ic_popup_sync);
                ((AnimationDrawable) execView.getDrawable()).start();
                outputView.setText(null);
                outputSize = 0;
                invalidateOptionsMenu();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                request(command);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_cmd, menu);
        if (execView.isClickable()) {
            menu.removeItem(R.id.action_stop);
            menu.findItem(R.id.action_reset).getIcon().setTint(mColorControlNormal);
            if (shouldUpdatePortal()) {
                addPortal(menu);
            }
            if (BreventActivity.isGenuineMotionelf(this)) {
                addMotionelf(menu);
            }
        } else {
            menu.removeItem(R.id.action_reset);
            menu.findItem(R.id.action_stop).getIcon().setTint(mColorControlNormal);
        }
        return true;
    }

    private void addPortal(Menu menu) {
        String command;
        try {
            String httpsUrl = HideApiOverride.getCaptivePortalHttpsUrl();
            command = "settings put global " + httpsUrl + " https://www.google.cn/generate_204";
        } catch (LinkageError e) {
            String server = HideApiOverride.getCaptivePortalServer();
            command = "settings put global " + server + " www.google.cn";
        }
        menu.add(Menu.NONE, R.string.cmd_menu_portal, Menu.NONE, R.string.cmd_menu_portal)
                .setIntent(getCommandIntent(command));
    }

    private String getGlobalString(String name) {
        return Settings.Global.getString(getContentResolver(), name);
    }

    private boolean shouldUpdatePortal() {
        if (TextUtils.isEmpty(getString(R.string.cmd_menu_portal))) {
            return false;
        }
        String key;
        try {
            key = HideApiOverride.getCaptivePortalHttpsUrl();
        } catch (LinkageError ignore) {
            key = HideApiOverride.getCaptivePortalServer();
        }
        return TextUtils.isEmpty(getGlobalString(key));
    }

    private void addMotionelf(Menu menu) {
        final String packageName = BreventActivity.MOTIONELF_PACKAGE;
        final String command = "path=/data/local/tmp/motionelf_server; " +
                "rm -rf $path; " +
                "cp /data/data/" + packageName + "/files/motionelf_server $path; " +
                "chmod 0755 $path; " +
                "$path &";
        addCommand(menu, packageName, command);
    }

    private boolean addCommand(Menu menu, String packageName, String command) {
        PackageManager packageManager = getPackageManager();
        if (packageManager.getLaunchIntentForPackage(packageName) == null) {
            return false;
        }
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(packageName, 0);
            String label = AppsLabelLoader.loadLabel(packageManager, packageInfo);
            MenuItem item = menu.add(Menu.NONE, packageInfo.applicationInfo.uid, Menu.NONE, label);
            item.setIntent(getCommandIntent(command));
            return true;
        } catch (PackageManager.NameNotFoundException ignore) {
            // ignore
            return false;
        }
    }

    private Intent getCommandIntent(String command) {
        return new Intent().putExtra(BreventIntent.EXTRA_COMMAND, command);
    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        BreventApplication application = (BreventApplication) getApplication();
        if (!application.checkPort()) {
            startBrevent(true);
        } else {
            setCommand(getIntent());
        }
    }

    private void startBrevent(boolean checked) {
        BreventApplication application = (BreventApplication) getApplication();
        if (checked || !application.checkPort()) {
            startActivity(new Intent(this, BreventActivity.class));
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        if (execView.isClickable()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        stopIfNeeded();
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        stopIfNeeded();
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (execView.isClickable()) {
                    startBrevent(false);
                }
                return true;
            case R.id.action_reset:
                doRest();
                return true;
            case R.id.action_stop:
                doStop();
                return true;
            case R.id.action_batterystats:
                setCommand(getCommandIntent("dumpsys batterystats > /sdcard/batterystats.txt"));
                return true;
            default:
                setCommand(item.getIntent());
                return super.onOptionsItemSelected(item);
        }
    }

    private void stopIfNeeded() {
        if (!execView.isClickable()) {
            reset(false);
            request("");
        }
    }

    private void doRest() {
        if (execView.isClickable()) {
            reset(true);
        }
    }

    private void doStop() {
        if (!execView.isClickable()) {
            request("");
        }
    }


    private void setCommand(Intent intent) {
        if (intent != null && execView.isClickable()) {
            String command = intent.getStringExtra(BreventIntent.EXTRA_COMMAND);
            if (!TextUtils.isEmpty(command) && !command.equals(commandView.getText().toString())) {
                commandView.setText(command);
                commandView.setSelection(command.length());
                outputView.setText(null);
            }
        }
    }

    private boolean isInvalid(String[] commands) {
        for (String command : commands) {
            if (isInvalid(command.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isInvalid(String command) {
        if (command.contains(" ")) {
            return false;
        } else if (command.equals("sh") || command.equals("su")) {
            return true;
        } else if (command.endsWith("/sh") || command.endsWith("/su")) {
            return true;
        } else {
            return false;
        }
    }

    private void reset(boolean clear) {
        if (clear) {
            commandView.getText().clear();
            outputView.setText(null);
        }
        commandView.setEnabled(true);
        execView.setImageResource(R.drawable.ic_send_black_24dp);
        execView.setClickable(true);
        invalidateOptionsMenu();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void request(final String command) {
        BreventCmdRequest cmdRequest = new BreventCmdRequest(command, true);
        cmdRequest.token = ((BreventApplication) getApplication()).getToken();
        workHandler.removeMessages(REQUEST);
        workHandler.obtainMessage(REQUEST, cmdRequest).sendToTarget();
    }

    private void handleResponse(String output, boolean done) {
        String extra = "";
        outputSize += output.length();
        int remain = MAX_SIZE - outputView.getText().length();
        UILog.d("output size: " + outputSize + ", done: " + done);
        if (done) {
            if (remain <= 0) {
                String error = getResources().getString(R.string.cmd_too_large,
                        commandView.getText(), outputSize, MAX_SIZE);
                commandView.setText(error);
                commandView.setSelection(error.length());
            }

            int count = Math.min(commandView.getLineCount(), commandView.getMaxLines());
            StringBuilder sb = new StringBuilder(count);
            for (int i = 0; i < count; ++i) {
                sb.append(System.lineSeparator());
            }
            extra = sb.toString();
            reset(false);
        }
        if (remain > 0) {
            outputView.append(output.substring(0, Math.min(remain, output.length())) + extra);
        }
    }

    static void clear(StringWriter sw) {
        StringBuffer buffer = sw.getBuffer();
        buffer.setLength(0);
        buffer.trimToSize();
    }

    static class MainHandler extends Handler {

        private final WeakReference<BreventCmd> mReference;

        private final StringWriter sw = new StringWriter();

        MainHandler(BreventCmd activity) {
            super(activity.getMainLooper());
            mReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case RESPONSE:
                    handleResponse((String) message.obj);
                    break;
                case PROTOCOL:
                    handleProtocol((BreventProtocol) message.obj);
                    break;
                case EXCEPTION:
                    handleException((Exception) message.obj);
                    break;
                case OUTPUT:
                    handleOutput(false);
                    break;
                default:
                    break;
            }

        }

        private void handleResponse(String output, boolean done) {
            synchronized (sw) {
                sw.write(output);
            }
            if (done) {
                handleOutput(true);
            } else {
                sendEmptyMessageDelayed(OUTPUT, LATER);
            }
        }

        private void handleResponse(String output) {
            handleResponse(output, false);
        }

        private void handleProtocol(BreventProtocol response) {
            if (response instanceof BreventCmdResponse) {
                if (((BreventCmdResponse) response).done) {
                    handleResponse("", true);
                }
            } else {
                UILog.d("response: " + response);
                handleResponse("", true);
            }
        }

        private void handleException(Exception exception) {
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            handleResponse(sw.toString(), true);
        }

        private void handleOutput(boolean done) {
            removeMessages(OUTPUT);
            String output;
            synchronized (sw) {
                output = sw.toString();
                clear(sw);
            }
            BreventCmd breventCmd = mReference.get();
            if (breventCmd != null && !breventCmd.isStopped()) {
                breventCmd.handleResponse(output, done);
            }
        }

    }

    static class WorkHandler extends Handler {

        private static final int TIMEOUT = 5000;

        private final Handler mainHandler;

        private final WeakReference<BreventCmd> mReference;

        WorkHandler(BreventCmd breventCmd, Handler handler) {
            super(newLooper());
            mReference = new WeakReference<>(breventCmd);
            mainHandler = handler;
        }

        private static Looper newLooper() {
            HandlerThread thread = new HandlerThread("BreventCmd");
            thread.start();
            return thread.getLooper();
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case REQUEST:
                    handleRequests((BreventCmdRequest) message.obj);
                    break;
                default:
                    break;
            }

        }

        private void handleRequests(BreventCmdRequest request) {
            if (request.force) {
                removeMessages(REQUEST);
            }
            try {
                handleRequest(request);
            } catch (IOException e) {
                sendException(e);
            }
        }

        private void handleRequest(BreventCmdRequest request) throws IOException {
            StringWriter writer = new StringWriter();

            BreventCmdRequest cmdRequest = request;
            BreventProtocol rawResponse = null;
            boolean shouldBreak = false;
            while (!shouldBreak) {
                rawResponse = doRequest(cmdRequest);
                if (rawResponse instanceof BreventCmdResponse) {
                    BreventCmdResponse response = (BreventCmdResponse) rawResponse;
                    cmdRequest = new BreventCmdRequest(response.request, false);
                    cmdRequest.token = response.token;
                    writer.write(response.output);
                    if (response.remain == 0) {
                        sendResponse(writer);
                        if (!response.done) {
                            sendMessageDelayed(obtainMessage(REQUEST, cmdRequest), LATER);
                        }
                        shouldBreak = true;
                    }
                } else {
                    UILog.d("response: " + rawResponse);
                    shouldBreak = true;
                }
                BreventCmd breventCmd = mReference.get();
                if (breventCmd == null || breventCmd.isStopped()) {
                    shouldBreak = true;
                } else {
                    ((BreventApplication) breventCmd.getApplication()).setToken(rawResponse.token);
                }
            }

            sendResponse(writer);
            sendResponse(rawResponse);
        }

        private void sendResponse(StringWriter writer) {
            String output = writer.toString();
            clear(writer);
            if (!output.isEmpty()) {
                mainHandler.sendMessage(obtainMessage(RESPONSE, output));
            }
        }

        private void sendResponse(BreventProtocol protocol) {
            mainHandler.sendMessage(obtainMessage(PROTOCOL, protocol));
        }

        private void sendException(IOException exception) {
            mainHandler.sendMessage(obtainMessage(EXCEPTION, exception));
        }

        BreventProtocol doRequest(BreventCmdRequest request) throws IOException {
            final BreventProtocol response;
            try (
                    Socket socket = new Socket(BreventProtocol.HOST, BreventProtocol.PORT);
                    DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                    DataInputStream is = new DataInputStream(socket.getInputStream())

            ) {
                socket.setSoTimeout(TIMEOUT);
                BreventProtocol.writeTo(request, os);
                os.flush();
                response = BreventProtocol.readFrom(is);
                return response;
            }
        }

    }

}
