package android.content.pm;

import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;

public interface IPackageManager {

    boolean isPackageAvailable(String packageName, int userId) throws RemoteException;

    PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException;

    ApplicationInfo getApplicationInfo(String packageName, int flags, int userId)
            throws RemoteException;

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
     * This implements getInstalledApplications via a "last returned row"
     * mechanism that is not exposed in the API. This is to get around the IPC
     * limit that kicks in when flags are included that bloat up the data
     * returned.
     */
    ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId)
            throws RemoteException;

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


    ParceledListSlice queryIntentActivities(Intent intent, String resolvedType, int flags, int userId)
            throws RemoteException;

    /**
     * @deprecated since api-23
     */
    int checkPermission(String permName, String pkgName) throws RemoteException;

    @RequiresApi(Build.VERSION_CODES.M)
    int checkPermission(String permName, String pkgName, int userId) throws RemoteException;

    /**
     * @deprecated since api-23
     */
    void grantPermission(String pkgName, String permName) throws RemoteException;

    @RequiresApi(Build.VERSION_CODES.M)
    void grantRuntimePermission(String pkgName, String permName, int userId)
            throws RemoteException;

    void setApplicationEnabledSetting(String packageName, int newState, int flags,
                                      int userId, String callingPackage)
            throws RemoteException;

    int getApplicationEnabledSetting(String packageName, int userId)
            throws RemoteException;

    String getPermissionControllerPackageName() throws RemoteException;

    String getServicesSystemSharedLibraryPackageName() throws RemoteException;

    String getSharedSystemSharedLibraryPackageName() throws RemoteException;

    class Stub {

        public static IPackageManager asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}