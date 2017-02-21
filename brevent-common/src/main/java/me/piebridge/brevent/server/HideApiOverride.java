package me.piebridge.brevent.server;

import android.app.AppGlobals;
import android.content.pm.PackageInfo;
import android.os.Process;
import android.os.RemoteException;

/**
 * Created by thom on 2017/2/17.
 */
public class HideApiOverride {

    private HideApiOverride() {

    }

    public static int uidForData() {
        int uid = Process.myUid();
        if (uid == 0) {
            return Process.SHELL_UID;
        } else {
            return uid;
        }
    }

    public static String[] getPackagesForUid(int uid) throws RemoteException {
        return AppGlobals.getPackageManager().getPackagesForUid(uid);
    }

    public static PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException {
        return AppGlobals.getPackageManager().getPackageInfo(packageName, flags, userId);
    }

}
