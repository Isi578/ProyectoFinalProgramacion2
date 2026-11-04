package com.hoteluq.application.reservation;

public final class RoomUnavailableException extends ReservationException {
    public RoomUnavailableException(String message) { super(message); }
}