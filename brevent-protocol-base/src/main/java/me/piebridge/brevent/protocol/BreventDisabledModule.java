package me.piebridge.brevent.protocol;

import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Objects;

/**
 * 1. 尽量不要持久化 token，应当放在内存等不易获取的地方<br>
 * 2. 除 IOException 外，还应当捕获 SecurityException<br>
 * Created by thom on 2018/2/25.
 */
@WorkerThread
public class BreventDisabledModule {

    private static final int TIMEOUT = 5000;

    private static String token;

    /**
     * 获取 token
     *
     * @return token
     */
    public static String getToken() {
        return token;
    }

    /**
     * 设定 token
     *
     * @param token
     */
    public static void setToken(String token) {
        BreventDisabledModule.token = token;
    }

    /**
     * 检测停用应用支持状态
     *
     * @return true 或 false
     * @throws IOException
     */
    public static boolean isAvailable() throws IOException {
        BaseBreventProtocol response = request(new BreventDisabledStatus(true));
        return response instanceof BreventDisabledStatus
                && ((BreventDisabledStatus) response).enabled;
    }

    /**
     * 获取用户停用（disabled-user）状态的应用
     *
     * @param uid UserHandler.getIdentifier()，可以间接使用 hashCode 或 toString 获取
     * @return
     * @throws IOException
     */
    @Nullable
    public static List<String> getDisabledPackages(int uid) throws IOException {
        BaseBreventProtocol response = request(new BreventDisabledPackagesRequest(uid));
        if (response instanceof BreventDisabledPackagesResponse) {
            return ((BreventDisabledPackagesResponse) response).packageNames;
        } else {
            return null;
        }
    }

    /**
     * 查看应用是否状态是否为用户停用（disabled-user）
     *
     * @param packageName 应用名包
     * @param uid         UserHandler.getIdentifier()，可以间接使用 hashCode 或 toString 获取
     * @return true 是，false 否
     * @throws IOException
     */
    public static boolean isDisabled(String packageName, int uid) throws IOException {
        BaseBreventProtocol response = request(new BreventDisabledGetState(packageName, uid, false));
        return response instanceof BreventDisabledGetState &&
                ((BreventDisabledGetState) response).disabled;
    }

    /**
     * 更新应用状态
     *
     * @param packageName 应用包名
     * @param uid         UserHandler.getIdentifier()，可以间接使用 hashCode 或 toString 获取
     * @param enable      true 设置为 enabled，false 设置为 disabled-user
     * @return true 设置成功，false 设置失败
     * @throws IOException
     */
    public static boolean setPackageEnabled(String packageName, int uid, boolean enable)
            throws IOException {
        BaseBreventProtocol response = request(new BreventDisabledSetState(packageName, uid, enable));
        return response instanceof BreventDisabledSetState &&
                ((BreventDisabledSetState) response).enable;
    }

    private static BaseBreventProtocol request(BaseBreventProtocol request) throws IOException {
        request.token = token;
        try (
                Socket socket = new Socket(BaseBreventProtocol.HOST, BaseBreventProtocol.PORT);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                DataInputStream is = new DataInputStream(socket.getInputStream())
        ) {
            socket.setSoTimeout(TIMEOUT);
            BaseBreventProtocol.writeTo(request, os);
            os.flush();
            BaseBreventProtocol response = BaseBreventProtocol.readFromBase(is);
            if (response == BaseBreventOK.INSTANCE) {
                throw new SecurityException("no permission");
            }
            if (response != null
                    && !TextUtils.isEmpty(response.token)
                    && !Objects.equals(token, response.token)) {
                token = response.token;
            }
            return response;
        }
    }

}
