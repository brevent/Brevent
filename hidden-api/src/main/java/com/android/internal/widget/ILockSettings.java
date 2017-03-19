package com.android.internal.widget;

import android.os.IBinder;
import android.os.RemoteException;

/**
 * Created by thom on 2017/3/19.
 */
public interface ILockSettings {

    String getString(String key, String defaultValue, int userId) throws RemoteException;

    abstract class Stub {

        public static ILockSettings asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}
