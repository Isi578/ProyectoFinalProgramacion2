package com.hoteluq.application.reservation;

import com.hoteluq.domain.model.Reservation;

import java.math.BigDecimal;

public final class CancellationResult {
    private final Reservation reservation;
    private final BigDecimal refundAmount;
    private final String reason;

    public CancellationResult(Reservation reservation, BigDecimal refundAmount, String reason) {
        this.reservation = reservation;
        this.refundAmount = refundAmount;
        this.reason = reason;
    }

    public Reservation getReservation() { return reservation; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public String getReason() { return reason; }
}