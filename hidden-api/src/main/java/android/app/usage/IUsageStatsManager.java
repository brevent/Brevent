package android.app.usage;

import android.content.pm.ParceledListSlice;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;

public interface IUsageStatsManager {

    /**
     * since api-23
     */
    @RequiresApi(Build.VERSION_CODES.M)
    void setAppInactive(String packageName, boolean inactive, int userId) throws RemoteException;

    /**
     * since api-23
     */
    @RequiresApi(Build.VERSION_CODES.M)
    boolean isAppInactive(String packageName, int userId) throws RemoteException;

    ParceledListSlice queryUsageStats(int bucketType, long beginTime, long endTime,
                                      String callingPackage);

    abstract class Stub {

        public static IUsageStatsManager asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}