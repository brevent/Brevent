package android.app;

import android.os.IBinder;
import android.os.RemoteException;

/**
 * Created by thom on 2017/3/18.
 */
public interface IAlarmManager {

    AlarmManager.AlarmClockInfo getNextAlarmClock(int userId) throws RemoteException;

    abstract class Stub {

        public static IAlarmManager asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}
