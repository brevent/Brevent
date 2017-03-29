package android.content;

import android.os.Bundle;
import android.os.RemoteException;

/**
 * Created by thom on 2016/10/23.
 */
public interface IIntentReceiver {

    void performReceive(Intent intent, int resultCode, String data,
                        Bundle extras, boolean ordered, boolean sticky, int sendingUser)
            throws RemoteException;

    abstract class Stub implements IIntentReceiver {

    }

}