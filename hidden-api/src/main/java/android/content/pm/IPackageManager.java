package android.content.pm;

import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;

public interface IPackageManager {

    boolean isPackageAvailable(String packageName, int userId) throws RemoteException;

    PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException;

    /**
     * @deprecated since api-24
     */
    int getPackageUid(String packageName, int userId) throws RemoteException;

    /**
     * since api-24
     */
    @RequiresApi(Build.VERSION_CODES.N)
    int getPackageUid(String packageName, int flags, int userId) throws RemoteException;

    /**
     * This implements getInstalledPackages via a "last returned row"
     * mechanism that is not exposed in the API. This is to get around the IPC
     * limit that kicks in when flags are included that bloat up the data
     * returned.
     */
    ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId) throws RemoteException;

    ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId)
            throws RemoteException;

    /**
     * Set whether the given package should be considered stopped, making
     * it not visible to implicit intents that filter out stopped packages.
     */
    void setPackageStoppedState(String packageName, boolean stopped, int userId)
            throws RemoteException;

    /**
     * This implements getPackagesHoldingPermissions via a "last returned row"
     * mechanism that is not exposed in the API. This is to get around the IPC
     * limit that kicks in when flags are included that bloat up the data
     * returned.
     */
    ParceledListSlice<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags,
                                                                 int userId) throws RemoteException;

    ParceledListSlice queryIntentReceivers(Intent intent, String resolvedType, int flags, int userId)
            throws RemoteException;

    class Stub {

        public static IPackageManager asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}