package android.app;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;

import java.util.List;

/**
 * Created by thom on 2016/11/22.
 */
public interface IActivityManager {

    void forceStopPackage(String packageName, int userId) throws RemoteException;

    /**
     * @deprecated since api-23
     */
    @Deprecated
    int broadcastIntent(IApplicationThread caller, Intent intent,
                        String resolvedType, IIntentReceiver resultTo, int resultCode,
                        String resultData, Bundle map, String requiredPermissions,
                        int appOp, boolean serialized, boolean sticky, int userId)
            throws RemoteException;

    /**
     * since api-23
     */
    @RequiresApi(Build.VERSION_CODES.M)
    int broadcastIntent(IApplicationThread caller, Intent intent,
                        String resolvedType, IIntentReceiver resultTo, int resultCode,
                        String resultData, Bundle map, String[] requiredPermissions,
                        int appOp, Bundle options, boolean serialized, boolean sticky, int userId)
            throws RemoteException;

    List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses() throws RemoteException;

    @RequiresApi(Build.VERSION_CODES.N)
    ParceledListSlice getRecentTasks(int maxNum, int flags, int userId) throws RemoteException;

    List<ActivityManager.RunningServiceInfo> getServices(int maxNum, int flags)
            throws RemoteException;

    int stopService(IApplicationThread caller, Intent service, String resolvedType, int userId)
            throws RemoteException;

    int startActivityAsUser(IApplicationThread caller, String callingPackage,
                            Intent intent, String resolvedType, IBinder resultTo, String resultWho,
                            int requestCode, int flags, ProfilerInfo profilerInfo,
                            Bundle options, int userId) throws RemoteException;

}
