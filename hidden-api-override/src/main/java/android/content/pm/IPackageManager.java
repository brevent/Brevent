package android.content.pm;

import android.os.RemoteException;

/**
 * Created by thom on 2017/2/21.
 */
public interface IPackageManager {

    String[] getPackagesForUid(int uid) throws RemoteException;

    PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException;

}
