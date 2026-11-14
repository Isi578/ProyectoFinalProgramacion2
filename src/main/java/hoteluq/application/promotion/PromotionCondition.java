package com.hoteluq.application.promotion;

import com.hoteluq.application.reservation.PromotionRejectedException;

/**
 * One eligibility rule of a promotion (Strategy). A promotion keeps a list of them and
 * checks each one in order; the first that is not met rejects the promotion with its reason.
 */
public interface PromotionCondition {
    /** @throws PromotionRejectedException when the condition is not met */
    void check(PromotionContext context);
}