package android.os;

/**
 * Created by thom on 2017/3/4.
 */
public interface IDeviceIdleController {

    String[] getFullPowerWhitelist() throws RemoteException;

    String[] getSystemPowerWhitelist() throws RemoteException;

    class Stub {

        public static IDeviceIdleController asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}
