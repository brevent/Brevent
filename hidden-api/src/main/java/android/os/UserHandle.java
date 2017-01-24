package android.os;

import android.support.annotation.RequiresApi;

/**
 * Created by thom on 2017/2/12.
 */
public class UserHandle {

    /**
     * @hide A user id constant to indicate the "owner" user of the device
     * @deprecated Consider using either {@link UserHandle#USER_SYSTEM} constant or
     * check the target user's flag {@link android.content.pm.UserInfo#isAdmin}.
     */
    public static int USER_OWNER = 0;

    /**
     * @hide A user id constant to indicate the "system" user of the device
     */
    @RequiresApi(Build.VERSION_CODES.N)
    public static int USER_SYSTEM = 0;

}
