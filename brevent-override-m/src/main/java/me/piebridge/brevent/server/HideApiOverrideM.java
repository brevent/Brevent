package me.piebridge.brevent.server;

import android.app.IActivityManager;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageParser;

import java.util.List;

/**
 * hide api for m
 * Created by thom on 2017/2/22.
 */
public class HideApiOverrideM {

    private HideApiOverrideM() {

    }

    @SuppressWarnings("unchecked")
    public static List getRecentTasks(IActivityManager am, int maxNum, int flags, int userId) {
        return am.getRecentTasks(maxNum, flags, userId);
    }

    public static void collectCertificates(PackageParser.Package pkg, int flags) throws PackageParser.PackageParserException {
        PackageParser packageParser = new PackageParser();
        packageParser.collectCertificates(pkg, flags);
    }

    public static List queryIntentReceivers(IPackageManager pm, Intent intent, int uid) {
        return pm.queryIntentReceivers(intent, null, 0, uid);
    }

}
