package android.webkit;

import android.os.IBinder;
import android.os.RemoteException;

/**
 * Created by thom on 2018/2/19.
 */
public interface IWebViewUpdateService {

    /**
     * Used by Settings to determine whether a certain package can be enabled/disabled by the user -
     * the package should not be modifiable in this way if it is a fallback package.
     */
    boolean isFallbackPackage(String packageName) throws RemoteException;

    class Stub {

        public static IWebViewUpdateService asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}
