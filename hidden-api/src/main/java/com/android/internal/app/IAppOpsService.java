package com.android.internal.app;

import android.app.AppOpsManager;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.List;

public interface IAppOpsService {

    void setMode(int code, int uid, String packageName, int mode) throws RemoteException;

    List<AppOpsManager.PackageOps> getPackagesForOps(int[] ops) throws RemoteException;

    class Stub {

        public static IAppOpsService asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}