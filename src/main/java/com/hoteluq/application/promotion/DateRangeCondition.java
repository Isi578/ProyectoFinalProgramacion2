package com.hoteluq.application.promotion;

import com.hoteluq.application.reservation.PromotionRejectedException;

import java.time.LocalDateTime;

/** The check-in must fall inside the promotion's validity window. */
public final class DateRangeCondition implements PromotionCondition {
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;

    public DateRangeCondition(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null || endsAt == null || endsAt.isBefore(startsAt)) {
            throw new IllegalArgumentException("Promotion dates must be set and end after they start.");
        }
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    @Override
    public void check(PromotionContext context) {
        if (context.checkInAt().isBefore(startsAt) || context.checkInAt().isAfter(endsAt)) {
            throw new PromotionRejectedException("The promotion is only valid for stays starting between "
                    + startsAt.toLocalDate() + " and " + endsAt.toLocalDate() + ".");
        }
    }
}