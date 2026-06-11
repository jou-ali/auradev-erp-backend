package com.auradev.erp.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stateless monetary arithmetic helpers used throughout the ERP.
 *
 * <p>All methods accept and return {@link BigDecimal}; {@code null} arguments
 * will throw {@link NullPointerException} (use non-null inputs at all call
 * sites).</p>
 *
 * <p>This class must never be instantiated.</p>
 */
public final class MoneyUtils {

    private static final int SCALE_MONEY  = 2;
    private static final int SCALE_QTY    = 3;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private MoneyUtils() {
    }

    // -------------------------------------------------------------------------
    // Core rounding helpers
    // -------------------------------------------------------------------------

    /**
     * Round {@code v} to 2 decimal places using {@link RoundingMode#HALF_UP}.
     *
     * @param v the value to round
     * @return {@code v} rounded to 2 dp
     */
    public static BigDecimal roundHalfUp2(BigDecimal v) {
        return v.setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    /**
     * Round {@code v} to 3 decimal places using {@link RoundingMode#HALF_UP}.
     *
     * <p>Used for stock quantities that carry 3-dp precision (e.g. litres,
     * kilograms).</p>
     *
     * @param v the value to round
     * @return {@code v} rounded to 3 dp
     */
    public static BigDecimal roundHalfUp3(BigDecimal v) {
        return v.setScale(SCALE_QTY, RoundingMode.HALF_UP);
    }

    // -------------------------------------------------------------------------
    // Percentage
    // -------------------------------------------------------------------------

    /**
     * Apply a percentage {@code rate} to {@code value}, rounded to 2 dp.
     *
     * <p>Formula: {@code value × rate ÷ 100}, rounded HALF_UP to 2 dp.</p>
     *
     * <p>Typical usage — tax calculation:</p>
     * <pre>{@code
     *   BigDecimal gst = MoneyUtils.pct(lineAmount, new BigDecimal("18")); // 18 % GST
     * }</pre>
     *
     * @param value the base amount
     * @param rate  the percentage rate (e.g. {@code 18} for 18 %)
     * @return the percentage amount rounded to 2 dp
     */
    public static BigDecimal pct(BigDecimal value, BigDecimal rate) {
        return value.multiply(rate)
                    .divide(HUNDRED, SCALE_MONEY, RoundingMode.HALF_UP);
    }

    // -------------------------------------------------------------------------
    // Whole-rupee rounding
    // -------------------------------------------------------------------------

    /**
     * Round {@code v} to the nearest whole rupee (0 decimal places, HALF_UP).
     *
     * <p>The round-off amount (difference between the rounded and original
     * value) must be tracked by the caller:</p>
     * <pre>{@code
     *   BigDecimal rounded  = MoneyUtils.toWhole(invoice.getNetAmount());
     *   BigDecimal roundOff = rounded.subtract(invoice.getNetAmount()); // positive or negative
     *   invoice.setRoundOff(roundOff);
     *   invoice.setNetPayable(rounded);
     * }</pre>
     *
     * @param v the monetary value to round
     * @return {@code v} rounded to 0 decimal places using HALF_UP
     */
    public static BigDecimal toWhole(BigDecimal v) {
        return v.setScale(0, RoundingMode.HALF_UP);
    }
}
