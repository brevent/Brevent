package me.piebridge.stats;

import android.content.Context;

import com.crashlytics.android.answers.AddToCartEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.InviteEvent;
import com.crashlytics.android.answers.LoginEvent;

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

    public static void logInvite(String mode, Map<String, String> attributes) {
        try {
            InviteEvent inviteEvent = new InviteEvent().putMethod(mode);
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                inviteEvent.putCustomAttribute(entry.getKey(), entry.getValue());
            }
            Answers.getInstance().logInvite(inviteEvent);
        } catch (IllegalStateException e) { // NOSONAR
            // do nothing
        }
    }

    public static void logLogin(String mode, Map<String, String> attributes) {
        try {
            LoginEvent loginEvent = new LoginEvent().putMethod(mode).putSuccess(true);
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                loginEvent.putCustomAttribute(entry.getKey(), entry.getValue());
            }
            Answers.getInstance().logLogin(loginEvent);
        } catch (IllegalStateException e) { // NOSONAR
            // do nothing
        }
    }

}
