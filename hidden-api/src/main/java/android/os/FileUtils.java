package android.os;

/**
 * @hide Created by thom on 2017/3/17.
 */
public class FileUtils {

    public static int S_IRWXU = 00700;
    public static int S_IRUSR = 00400;
    public static int S_IWUSR = 00200;
    public static int S_IXUSR = 00100;

    public static int S_IRWXG = 00070;
    public static int S_IRGRP = 00040;
    public static int S_IWGRP = 00020;
    public static int S_IXGRP = 00010;

    public static int S_IRWXO = 00007;
    public static int S_IROTH = 00004;
    public static int S_IWOTH = 00002;
    public static int S_IXOTH = 00001;

    public static int setPermissions(String path, int mode, int uid, int gid) {
        throw new UnsupportedOperationException();
    }

    public static int getUid(String path) {
        throw new UnsupportedOperationException();
    }

}
