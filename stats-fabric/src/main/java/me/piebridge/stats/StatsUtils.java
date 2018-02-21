package me.piebridge.stats;

import android.content.Context;
import android.os.Build;

import com.crashlytics.android.answers.AddToCartEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.InviteEvent;
import com.crashlytics.android.answers.LoginEvent;
import com.crashlytics.android.answers.ShareEvent;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

/**
 * Created by thom on 2017/12/10.
 */

public class StatsUtils {

    private StatsUtils() {

    }

    public static void init(Context context) {
        Fabric.with(context, new Answers());
    }

    public static void logGuide(String type, String installer) {
        try {
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Guide")
                    .putContentType(type)
                    .putContentId("guide-" + type)
                    .putCustomAttribute("installer", installer));
        } catch (IllegalStateException e) { // NOSONAR
            // do nothing
        }
    }


    public static void logDonate(String type, String mode, String installer) {
        try {
            Answers.getInstance().logAddToCart(new AddToCartEvent()
                    .putItemPrice(BigDecimal.ONE)
                    .putCurrency(Currency.getInstance("USD"))
                    .putItemName("Donate")
                    .putItemType(type)
                    .putItemId("donate-" + mode + "-" + type)
                    .putCustomAttribute("mode", mode)
                    .putCustomAttribute("installer", installer));
        } catch (IllegalStateException e) { // NOSONAR
            // do nothing
        }
    }

    public static void logInvite(Map<String, Object> attributes) {
        try {
            InviteEvent inviteEvent = new InviteEvent();
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    inviteEvent.putCustomAttribute(entry.getKey(), (String) value);
                } else if (value instanceof Number) {
                    inviteEvent.putCustomAttribute(entry.getKey(), (Number) value);
                }
            }
            Answers.getInstance().logInvite(inviteEvent);
        } catch (IllegalStateException e) { // NOSONAR
            // do nothing
        }
    }

    public static void logLogin(Map<String, Object> attributes) {
        try {
            LoginEvent loginEvent = new LoginEvent();
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    loginEvent.putCustomAttribute(entry.getKey(), (String) value);
                } else if (value instanceof Number) {
                    loginEvent.putCustomAttribute(entry.getKey(), (Number) value);
                }
            }
            Answers.getInstance().logLogin(loginEvent);
        } catch (IllegalStateException e) { // NOSONAR
            // do nothing
        }
    }

    public static void logShare() {
        try {
            Answers.getInstance().logShare(new ShareEvent()
                    .putContentId("share-" + Build.DEVICE + "-" + Build.VERSION.SDK_INT)
                    .putContentName(Build.MODEL)
                    .putContentType(Build.VERSION.RELEASE));
        } catch (IllegalThreadStateException e) { // NOSONAR
            // do nothing
        }
    }

}
