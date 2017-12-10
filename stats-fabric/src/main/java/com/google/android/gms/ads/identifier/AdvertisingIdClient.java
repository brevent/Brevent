package com.google.android.gms.ads.identifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Keep;
import android.text.TextUtils;

import java.util.UUID;

/**
 * Created by thom on 2017/7/17.
 */
@Keep
public class AdvertisingIdClient {

    public static Info getAdvertisingIdInfo(Context context) {
        SharedPreferences sp = context.getSharedPreferences("fake_gms_ads", 0);
        String uuid = sp.getString("advertising_id", "");
        if (TextUtils.isEmpty(uuid)) {
            uuid = UUID.randomUUID().toString();
            sp.edit().putString("advertising_id", uuid).apply();
        }
        return new Info(uuid);
    }

    @Keep
    public static class Info {

        private final String uuid;

        Info(String uuid) {
            this.uuid = uuid;
        }

        public String getId() {
            return uuid;
        }

        public boolean isLimitAdTrackingEnabled() {
            return false;
        }

    }

}
