package android.app.admin;

import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.List;

/**
 * Created by thom on 2017/3/18.
 */
public interface IDevicePolicyManager {

    List<ComponentName> getActiveAdmins(int userHandle) throws RemoteException;

    abstract class Stub {

        public static IDevicePolicyManager asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}
