package android.app;

import android.os.IBinder;
import android.os.RemoteException;

/**
 * Created by thom on 2017/6/24.
 */
public interface IWallpaperManager {

    WallpaperInfo getWallpaperInfo(int userId) throws RemoteException;

    abstract class Stub {

        public static IWallpaperManager asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }
    }

}
