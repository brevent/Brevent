package de.robv.android.xposed;

/**
 * Created by thom on 2017/2/18.
 */
public class XposedBridge {

    /**
     * @hide
     */
    public static class CopyOnWriteSortedSet<E> {

        public Object[] getSnapshot() {
            throw new UnsupportedOperationException();
        }

    }

}
