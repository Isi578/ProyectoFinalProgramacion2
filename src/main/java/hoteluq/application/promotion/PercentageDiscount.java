package com.hoteluq.application.promotion;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PercentageDiscount implements DiscountStrategy {
    private final BigDecimal rate;

    /** @param rate fraction between 0 (exclusive) and 0.5 (inclusive), e.g. 0.10 for 10% */
    public PercentageDiscount(BigDecimal rate) {
        if (rate == null || rate.signum() <= 0
                || rate.compareTo(DiscountStrategy.MAX_DISCOUNT_RATE) > 0) {
            throw new IllegalArgumentException(
                    "Discount rate must be greater than 0 and at most 0.50.");
        }
        this.rate = rate;
    }

    @Override
    public BigDecimal discountFor(BigDecimal subtotal) {
        return subtotal.multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal getRate() { return rate; }
}