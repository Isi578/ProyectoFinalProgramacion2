package com.hoteluq.application.reservation;

public final class CancellationNotAllowedException extends ReservationException {
    public CancellationNotAllowedException(String message) { super(message); }
}