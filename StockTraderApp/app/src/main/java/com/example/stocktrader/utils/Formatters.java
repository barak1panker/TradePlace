package com.example.stocktrader.utils;

import java.text.DecimalFormat;

/**
 * Centralized formatters - keep number/currency presentation consistent across the app.
 */
public class Formatters {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DecimalFormat PERCENT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat WHOLE = new DecimalFormat("#,##0");

    public static String money(double value) {
        return "$" + MONEY.format(value);
    }

    /** Money with a leading +/- (used for P&L). */
    public static String signedMoney(double value) {
        String s = MONEY.format(Math.abs(value));
        if (value >= 0) return "+$" + s;
        return "-$" + s;
    }

    public static String percent(double value) {
        String s = PERCENT.format(Math.abs(value));
        return (value >= 0 ? "+" : "-") + s + "%";
    }

    public static String whole(long value) {
        return WHOLE.format(value);
    }
}
