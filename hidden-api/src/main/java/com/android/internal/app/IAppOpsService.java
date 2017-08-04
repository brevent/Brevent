package com.android.internal.app;

import android.os.IBinder;
import android.os.RemoteException;

import java.util.List;

public interface IAppOpsService {

    int checkOperation(int code, int uid, String packageName) throws RemoteException;

    void setMode(int code, int uid, String packageName, int mode) throws RemoteException;

    List getPackagesForOps(int[] ops) throws RemoteException;

    class Stub {

        public static IAppOpsService asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}