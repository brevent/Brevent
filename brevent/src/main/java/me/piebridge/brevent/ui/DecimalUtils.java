package me.piebridge.brevent.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Created by thom on 2017/9/1.
 */
public class DecimalUtils {

    private static final String FORMAT = "#.#";

    private static RoundingMode MODE = new DecimalFormat(FORMAT).getRoundingMode();

    static String format(double d) {
        return new DecimalFormat(FORMAT).format(d);
    }

    static int intValue(double d) {
        return valueOf(d).intValue();
    }

    static boolean isPositive(double d) {
        BigDecimal b = valueOf(d);
        return b.compareTo(BigDecimal.ZERO) > 0;
    }

    static int add(double d1, double d2) {
        BigDecimal b1 = valueOf(d1);
        BigDecimal b2 = valueOf(d2);
        return b1.add(b2).intValue();
    }

    private static BigDecimal valueOf(double d) {
        return BigDecimal.valueOf(d).setScale(1, MODE);
    }

}
