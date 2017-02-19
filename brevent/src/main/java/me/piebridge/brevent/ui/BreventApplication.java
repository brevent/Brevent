package me.piebridge.brevent.ui;

import android.app.Application;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.Map;
import java.util.UUID;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;

/**
 * Created by thom on 2017/2/7.
 */
public class BreventApplication extends Application {

    private UUID mToken;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // disable hooks
            Field disableHooks = XposedBridge.class.getDeclaredField("disableHooks");
            disableHooks.setAccessible(true);
            disableHooks.set(null, true);

            // replace hooked method callbacks
            Field sHookedMethodCallbacks = XposedBridge.class.getDeclaredField("sHookedMethodCallbacks");
            sHookedMethodCallbacks.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Member, XposedBridge.CopyOnWriteSortedSet<XC_MethodHook>> map = (Map<Member, XposedBridge.CopyOnWriteSortedSet<XC_MethodHook>>) sHookedMethodCallbacks.get(null);
            for (XposedBridge.CopyOnWriteSortedSet<XC_MethodHook> hooked : map.values()) {
                Object[] snapshot = hooked.getSnapshot();
                int length = snapshot.length;
                for (int i = 0; i < length; ++i) {
                    snapshot[i] = XC_MethodReplacement.DO_NOTHING;
                }
            }
        } catch (Throwable t) { // NOSONAR
            // do nothing
        }
        mToken = UUID.randomUUID();
    }

    public UUID getToken() {
        return mToken;
    }

}
