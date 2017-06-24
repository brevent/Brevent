package android.app;

import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;

/**
 * Created by thom on 2017/6/24.
 */
public interface IWallpaperManager {

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    WallpaperInfo getWallpaperInfo(int userId) throws RemoteException;

    /**
     * deprecated since 7.1
     */
    @Deprecated
    WallpaperInfo getWallpaperInfo() throws RemoteException;

    abstract class Stub {

        public static IWallpaperManager asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }
    }

}
