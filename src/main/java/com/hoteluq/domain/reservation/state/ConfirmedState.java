package com.hoteluq.domain.reservation.state;

import com.hoteluq.domain.model.ReservationStatus;

public final class ConfirmedState implements ReservationState {
    @Override public ReservationStatus status() { return ReservationStatus.CONFIRMED; }
    @Override public boolean canTransitionTo(ReservationStatus next) {
        return next == ReservationStatus.CANCELLED;
    }
}