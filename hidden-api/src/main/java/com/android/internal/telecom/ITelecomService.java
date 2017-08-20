package com.android.internal.telecom;

import android.os.IBinder;
import android.os.RemoteException;

/**
 * Created by thom on 2017/8/20.
 */
public interface ITelecomService {

    int getCallState() throws RemoteException;

    abstract class Stub {

        public static ITelecomService asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}
