package me.piebridge.brevent.protocol;

import android.content.pm.ApplicationInfo;
import android.os.Parcel;

/**
 * Created by thom on 2018/2/28.
 */
public class BreventApplicationInfo extends ApplicationInfo {

    public BreventApplicationInfo(ApplicationInfo applicationInfo) {
        super(new DummyApplicationInfo(applicationInfo));
    }

    public BreventApplicationInfo(Parcel in) {
        super(new DummyApplicationInfo(in));
    }

    public boolean isInstantApp() {
        return true;
    }

    public void writeToParcel(Parcel dest) {
        DummyApplicationInfo.writeToParcel(this, dest);
    }

    private static class DummyApplicationInfo extends ApplicationInfo {

        DummyApplicationInfo(ApplicationInfo applicationInfo) {
            this.packageName = applicationInfo.packageName;

            this.flags = applicationInfo.flags;
            this.uid = applicationInfo.uid;
            this.targetSdkVersion = applicationInfo.targetSdkVersion;

            // loadLabel
            this.nonLocalizedLabel = applicationInfo.nonLocalizedLabel;
            this.labelRes = applicationInfo.labelRes;
            this.name = applicationInfo.name;

            // loadIcon
            this.icon = applicationInfo.icon;

            this.publicSourceDir = applicationInfo.publicSourceDir;
            this.splitPublicSourceDirs = applicationInfo.splitPublicSourceDirs;
        }

        public DummyApplicationInfo(Parcel in) {
            this.packageName = in.readString();

            this.flags = in.readInt();
            this.uid = in.readInt();
            this.targetSdkVersion = in.readInt();

            // loadLabel
            this.nonLocalizedLabel = in.readString();
            this.labelRes = in.readInt();
            this.name = in.readString();

            // loadIcon
            this.icon = in.readInt();

            this.publicSourceDir = in.readString();
            this.splitPublicSourceDirs = in.createStringArray();
        }

        public static void writeToParcel(ApplicationInfo applicationInfo, Parcel dest) {
            dest.writeString(applicationInfo.packageName);

            dest.writeInt(applicationInfo.flags);
            dest.writeInt(applicationInfo.uid);
            dest.writeInt(applicationInfo.targetSdkVersion);

            if (applicationInfo.nonLocalizedLabel == null) {
                dest.writeString(null);
            } else {
                dest.writeString(applicationInfo.nonLocalizedLabel.toString());
            }
            dest.writeInt(applicationInfo.labelRes);
            dest.writeString(applicationInfo.name);

            dest.writeInt(applicationInfo.icon);

            dest.writeString(applicationInfo.publicSourceDir);
            dest.writeStringArray(applicationInfo.splitPublicSourceDirs);
        }
    }

}
