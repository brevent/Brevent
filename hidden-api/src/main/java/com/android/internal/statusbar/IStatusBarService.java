package com.android.internal.statusbar;

import android.os.IBinder;
import android.os.RemoteException;

/**
 * Created by thom on 2017/7/26.
 */
public interface IStatusBarService {

    void expandNotificationsPanel() throws RemoteException;

    void collapsePanels() throws RemoteException;

    class Stub {

        public static IStatusBarService asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }

    }

}
