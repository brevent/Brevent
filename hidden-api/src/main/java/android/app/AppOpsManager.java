package android.app;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.List;

/**
 * Created by thom on 2016/11/24.
 */
public class AppOpsManager {

    public static final int MODE_ALLOWED = 0;
    public static final int MODE_IGNORED = 1;

    public static int OP_NONE = -1;

    public static int OP_POST_NOTIFICATION = 11;

    public static int OP_ACTIVATE_VPN = 47;

    @RequiresApi(Build.VERSION_CODES.N)
    public static int OP_RUN_IN_BACKGROUND = 63;

    public static String opToName(int op) {
        throw new UnsupportedOperationException();
    }

    public static int strDebugOpToOp(String op) {
        throw new UnsupportedOperationException();
    }

    /**
     * Class holding all of the operation information associated with an app.
     * @hide
     */
    public static class PackageOps {

        public String getPackageName() {
            throw new UnsupportedOperationException();
        }

        public int getUid() {
            throw new UnsupportedOperationException();
        }

        public List<OpEntry> getOps() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Class holding the information about one unique operation of an application.
     * @hide
     */
    public static class OpEntry {

        public int getOp() {
            throw new UnsupportedOperationException();
        }

        public int getMode() {
            throw new UnsupportedOperationException();
        }

        public long getTime() {
            throw new UnsupportedOperationException();
        }

        public long getRejectTime() {
            throw new UnsupportedOperationException();
        }

        public boolean isRunning() {
            throw new UnsupportedOperationException();
        }

        public int getDuration() {
            throw new UnsupportedOperationException();
        }

        public int getProxyUid() {
            throw new UnsupportedOperationException();
        }

        public String getProxyPackageName() {
            throw new UnsupportedOperationException();
        }

    }
}