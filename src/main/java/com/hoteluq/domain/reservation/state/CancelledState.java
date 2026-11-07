package com.hoteluq.domain.reservation.state;

import com.hoteluq.domain.model.ReservationStatus;

public final class CancelledState implements ReservationState {
    @Override public ReservationStatus status() { return ReservationStatus.CANCELLED; }
    @Override public boolean canTransitionTo(ReservationStatus next) { return false; }
}