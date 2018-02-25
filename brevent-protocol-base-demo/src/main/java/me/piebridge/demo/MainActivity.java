package me.piebridge.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import me.piebridge.brevent.protocol.BreventDisabledModule;

/**
 * Created by thom on 2018/2/25.
 */
public class MainActivity extends Activity {

    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        output = findViewById(R.id.output);
        new Thread(new Runnable() {
            @Override
            public void run() {
                check();
            }
        }).start();
    }

    private void check() {
        try {
            checkDisabled();
        } catch (IOException | SecurityException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            updateMessage(sw.toString());
        }
    }

    private void checkDisabled() throws IOException {
        final StringBuilder sb = new StringBuilder();
        if (checkStatus(sb)) {

            checkDisabledPackages(sb);

            final String packageName = "me.piebridge.brevent";

            enable(sb, packageName, false);
            checkDisabled(sb, packageName);
            checkDisabledPackages(sb);

            enable(sb, packageName, true);
            checkDisabled(sb, packageName);
            checkDisabledPackages(sb);

            sb.append("token（注意安全）: ");
            sb.append(BreventDisabledModule.getToken());
            sb.append("\n");
            updateMessage(sb);
        }
    }

    private boolean checkStatus(StringBuilder sb) throws IOException {
        sb.append("停用: ");
        boolean result = BreventDisabledModule.isAvailable();
        if (result) {
            sb.append("支持\n");
        } else {
            sb.append("不支持\n");
        }
        updateMessage(sb);
        return result;
    }

    private void enable(StringBuilder sb, String packageName, boolean enable) throws IOException {
        sb.append("设置");
        sb.append(packageName);
        sb.append("为");
        if (enable) {
            sb.append("启用");
        } else {
            sb.append("用户停用");
        }
        sb.append("状态: ");
        if (BreventDisabledModule.setPackageEnabled(packageName, 0, enable)) {
            sb.append("成功\n");
        } else {
            sb.append("失败\n");
        }
        updateMessage(sb);
    }

    private void updateMessage(StringBuilder sb) {
        updateMessage(sb.toString());
    }

    private void updateMessage(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                output.setText(s);
            }
        });
    }

    private void checkDisabled(StringBuilder sb, String packageName) throws IOException {
        sb.append(packageName);
        sb.append("是否用户停用: ");
        if (BreventDisabledModule.isDisabled(packageName, 0)) {
            sb.append("是\n");
        } else {
            sb.append("否\n");
        }
        updateMessage(sb);
    }

    private void checkDisabledPackages(StringBuilder sb) throws IOException {
        sb.append("停用应用: ");
        List<String> disabledPackages = BreventDisabledModule.getDisabledPackages(0);
        if (disabledPackages == null) {
            sb.append("空\n");
        } else {
            sb.append(disabledPackages);
            sb.append("\n");
        }
        updateMessage(sb);
    }

}
