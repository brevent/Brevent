package android.os;

import java.io.FileDescriptor;

/**
 * Created by thom on 2017/3/23.
 */

public interface IBinder {

    void shellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err,
                      String[] args, ShellCallback shellCallback, ResultReceiver resultReceiver) throws RemoteException;

}
