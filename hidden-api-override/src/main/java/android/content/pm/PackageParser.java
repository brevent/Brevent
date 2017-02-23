package android.content.pm;

/**
 * @hide Created by thom on 2017/2/21.
 */
public class PackageParser {

    public void collectCertificates(Package pkg, int flags) throws PackageParserException {
        throw new UnsupportedOperationException();
    }

    public final static class Package {

        public Signature[] mSignatures;

        public String baseCodePath;

        public Package(String packageName) {
            throw new UnsupportedOperationException();
        }

    }

    public static class PackageParserException extends Exception {

    }

}
