package com.hoteluq.application.promotion;

import java.math.BigDecimal;

/** Strategy: how much a promotion takes off a subtotal. */
public interface DiscountStrategy {
    /** Rule BR-A05: no discount may exceed 50% of the subtotal. */
    BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.50");

    BigDecimal discountFor(BigDecimal subtotal);
}