package android.content.pm;

/**
 * @hide Created by thom on 2017/2/21.
 */
public class PackageParser {

    private PackageParser() {

    }

    public static void collectCertificates(Package pkg, int parseFlags)
            throws PackageParserException {
        throw new UnsupportedOperationException();
    }

    public static class Package {

        public Signature[] mSignatures;

        public String baseCodePath;

        public Package(String packageName) {
            throw new UnsupportedOperationException();
        }

    }

    public static class PackageParserException extends Exception {

    }

}
