package android.app;

import java.util.List;

/**
 * Created by thom on 2016/11/24.
 */
public class AppOpsManager {

    /**
     * @hide No operation specified.
     */
    public static int OP_NONE = -1;

    /**
     * @hide Activate a VPN connection without user intervention.
     */
    public static int OP_ACTIVATE_VPN = 47;

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