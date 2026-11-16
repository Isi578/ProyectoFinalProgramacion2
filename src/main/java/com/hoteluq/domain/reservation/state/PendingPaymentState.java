package com.hoteluq.domain.reservation.state;

import com.hoteluq.domain.model.ReservationStatus;

public final class PendingPaymentState implements ReservationState {
    @Override public ReservationStatus status() { return ReservationStatus.PENDING_PAYMENT; }
    @Override public boolean canTransitionTo(ReservationStatus next) {
        return next == ReservationStatus.CONFIRMED || next == ReservationStatus.CANCELLED;
    }
}