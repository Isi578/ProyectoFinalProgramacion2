package com.hoteluq.application.reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class HotelCancellationPolicy implements CancellationPolicy {
    @Override
    public RefundDecision calculate(LocalDateTime cancelledAt, LocalDateTime checkInAt,
                                    BigDecimal originalAmount) {
        return new RefundDecision(originalAmount, "Reservation cancelled by the hotel.");
    }
}