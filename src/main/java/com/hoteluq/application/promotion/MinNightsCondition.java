package com.hoteluq.application.promotion;

import com.hoteluq.application.reservation.PromotionRejectedException;

/** The stay must last at least a minimum number of nights. */
public final class MinNightsCondition implements PromotionCondition {
    private final int minNights;

    public MinNightsCondition(int minNights) {
        if (minNights < 1) throw new IllegalArgumentException("Minimum nights must be positive.");
        this.minNights = minNights;
    }

    @Override
    public void check(PromotionContext context) {
        if (context.nights() < minNights) {
            throw new PromotionRejectedException(
                    "The promotion requires at least " + minNights + " nights.");
        }
    }
}