package com.hoteluq.application.reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CancellationPolicy {
    RefundDecision calculate(LocalDateTime cancelledAt, LocalDateTime checkInAt,
                             BigDecimal originalAmount);
}