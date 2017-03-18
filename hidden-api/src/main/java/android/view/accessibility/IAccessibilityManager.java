package android.view.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.List;

/**
 * Created by thom on 2017/3/18.
 */
public interface IAccessibilityManager {

    List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int feedbackType, int userId) throws RemoteException;

    abstract class Stub {

        public static IAccessibilityManager asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}
