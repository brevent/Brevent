package de.robv.android.xposed;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by thom on 2017/2/18.
 */
public class XposedBridge {

    /*package*/ static boolean disableHooks = false;

    private static Map<Member, CopyOnWriteSortedSet<XC_MethodHook>> sHookedMethodCallbacks = new HashMap<>();

    /** @hide */
    public static class CopyOnWriteSortedSet<E> {

        public Object[] getSnapshot() {
            throw new UnsupportedOperationException();
        }

    }

}
