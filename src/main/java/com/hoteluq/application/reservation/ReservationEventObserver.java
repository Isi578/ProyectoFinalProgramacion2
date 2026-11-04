package com.hoteluq.application.reservation;

public interface ReservationEventObserver {
    void onReservationEvent(ReservationEvent event);
}