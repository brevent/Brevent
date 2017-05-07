package android.os;

import android.support.annotation.RequiresApi;

/**
 * Created by thom on 2017/3/4.
 */
@RequiresApi(Build.VERSION_CODES.M)
public interface IDeviceIdleController {

    String[] getFullPowerWhitelist() throws RemoteException;

    class Stub {

        public static IDeviceIdleController asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}
