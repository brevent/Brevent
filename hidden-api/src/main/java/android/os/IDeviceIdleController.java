package android.os;

import android.annotation.TargetApi;

/**
 * Created by thom on 2017/3/4.
 */
@TargetApi(Build.VERSION_CODES.M)
public interface IDeviceIdleController {

    String[] getFullPowerWhitelist() throws RemoteException;

    class Stub {

        public static IDeviceIdleController asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}
