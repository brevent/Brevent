package android.content;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;

/**
 * Created by thom on 2017/3/18.
 */
public interface IContentProvider {

    Bundle call(String callingPkg, String method, @Nullable String arg, @Nullable Bundle extras) throws RemoteException;

}