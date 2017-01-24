package com.android.internal.app;

import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;

/**
 * Created by thom on 2016/12/30.
 */
public interface IBatteryStats {

    /**
     * since api-23
     */
    @RequiresApi(Build.VERSION_CODES.M)
    boolean isCharging() throws RemoteException;

    class Stub {

        public static IBatteryStats asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}