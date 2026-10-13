package com.fashion.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final BigDecimal ZERO = scale(BigDecimal.ZERO);

    private Money() {
    }

    public static BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal discountAmount(BigDecimal unitPrice, BigDecimal discountPercent, int quantity) {
        BigDecimal line = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return scale(line.multiply(discountPercent).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
    }

    public static BigDecimal lineTotal(BigDecimal unitPrice, BigDecimal discountPercent, int quantity) {
        BigDecimal line = scale(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        return scale(line.subtract(discountAmount(unitPrice, discountPercent, quantity)));
    }
}
