package me.piebridge.brevent.server;

import android.app.IActivityManager;
import android.content.pm.PackageParser;
import android.os.UserHandle;

import java.util.List;

/**
 * Created by thom on 2017/2/22.
 */
public class HideApiOverrideM {

    public static final int USER_OWNER = UserHandle.USER_OWNER;

    private HideApiOverrideM() {

    }

    @SuppressWarnings("unchecked")
    static List getRecentTasks(IActivityManager am, int maxNum, int flags, int userId) {
        return am.getRecentTasks(maxNum, flags, userId);
    }

    public static void collectCertificates(PackageParser.Package pkg, int flags) throws PackageParser.PackageParserException {
        PackageParser packageParser = new PackageParser();
        packageParser.collectCertificates(pkg, flags);
    }

}
