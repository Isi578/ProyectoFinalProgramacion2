package com.hoteluq.application.promotion;

import java.math.BigDecimal;

public final class FixedAmountDiscount implements DiscountStrategy {
    private final BigDecimal amount;

    public FixedAmountDiscount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Discount amount must be positive.");
        }
        this.amount = amount;
    }

    @Override
    public BigDecimal discountFor(BigDecimal subtotal) { return amount; }
}