package com.hoteluq.application.reservation;

import java.math.BigDecimal;

public final class RefundDecision {
    private final BigDecimal refundAmount;
    private final String reason;

    public RefundDecision(BigDecimal refundAmount, String reason) {
        if (refundAmount == null || refundAmount.signum() < 0) {
            throw new IllegalArgumentException("Refund amount cannot be negative.");
        }
        this.refundAmount = refundAmount;
        this.reason = reason;
    }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public String getReason() { return reason; }
}