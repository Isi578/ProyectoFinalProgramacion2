package com.hoteluq.application.reservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public final class StandardCancellationPolicy implements CancellationPolicy {
    private static final long HOURS_72_IN_MINUTES = 72L * 60L;
    private static final long HOURS_24_IN_MINUTES = 24L * 60L;

    @Override
    public RefundDecision calculate(LocalDateTime cancelledAt, LocalDateTime checkInAt,
                                    BigDecimal originalAmount) {
        Duration timeBeforeArrival = Duration.between(cancelledAt, checkInAt);

        if (timeBeforeArrival.compareTo(Duration.ofHours(72)) > 0) {
            return new RefundDecision(originalAmount, "Cancelled more than 72 hours before arrival.");
        }
        if (timeBeforeArrival.compareTo(Duration.ofHours(24)) >= 0) {
            BigDecimal refund = originalAmount.multiply(new BigDecimal("0.80"))
                    .setScale(2, RoundingMode.HALF_UP);
            return new RefundDecision(refund, "Cancelled between 24 and 72 hours before arrival.");
        }
        return new RefundDecision(BigDecimal.ZERO.setScale(2), "Cancelled less than 24 hours before arrival.");
    }
}