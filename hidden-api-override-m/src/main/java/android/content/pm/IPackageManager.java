package android.content.pm;

import android.content.Intent;

import java.util.List;

/**
 * Created by thom on 2017/3/3.
 */
public interface IPackageManager {

    List queryIntentReceivers(Intent intent, String resolvedType, int flags, int userId);

}
