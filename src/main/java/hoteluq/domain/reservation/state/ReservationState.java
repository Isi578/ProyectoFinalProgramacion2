package com.hoteluq.domain.reservation.state;

import com.hoteluq.domain.model.ReservationStatus;

public interface ReservationState {
    ReservationStatus status();
    boolean canTransitionTo(ReservationStatus next);
}