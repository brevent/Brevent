package com.android.internal.app;

import android.os.IBinder;
import android.os.RemoteException;

import java.util.List;

public interface IAppOpsService {

    int checkOperation(int code, int uid, String packageName) throws RemoteException;

    void setMode(int code, int uid, String packageName, int mode) throws RemoteException;

    List getPackagesForOps(int[] ops) throws RemoteException;

    List getOpsForPackage(int uid, String packageName, int[] ops) throws RemoteException;

    void resetAllModes(int reqUserId, String reqPackageName) throws RemoteException;

    class Stub {

        public static IAppOpsService asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}