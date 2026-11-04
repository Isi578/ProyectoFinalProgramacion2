package com.hoteluq.application.reservation;

import com.hoteluq.domain.model.Reservation;
import com.hoteluq.domain.model.ReservationStatus;

import java.time.LocalDateTime;

public final class ReservationEvent {
    public enum Type { CREATED, CANCELLED }

    private final Type type;
    private final Reservation reservation;
    private final ReservationStatus statusAtEvent;
    private final LocalDateTime occurredAt;

    public ReservationEvent(Type type, Reservation reservation, LocalDateTime occurredAt) {
        this.type = type;
        this.reservation = reservation;
        this.statusAtEvent = reservation.getStatus();
        this.occurredAt = occurredAt;
    }

    public Type getType() { return type; }
    public Reservation getReservation() { return reservation; }
    public ReservationStatus getStatusAtEvent() { return statusAtEvent; }
    public LocalDateTime getOccurredAt() { return occurredAt; }

    @Override
    public String toString() {
        return occurredAt + " | " + type + " | reservation="
                + reservation.getId() + " | status=" + statusAtEvent;
    }
}