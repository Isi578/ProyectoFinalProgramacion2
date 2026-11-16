package com.hoteluq.application.reservation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ReservationEventPublisher {
    private final List<ReservationEventObserver> observers = new CopyOnWriteArrayList<>();

    public void subscribe(ReservationEventObserver observer) {
        if (observer != null) observers.add(observer);
    }

    public void unsubscribe(ReservationEventObserver observer) {
        observers.remove(observer);
    }

    public void publish(ReservationEvent event) {
        for (ReservationEventObserver observer : observers) {
            try {
                observer.onReservationEvent(event);
            } catch (RuntimeException exception) {
                System.err.println("Reservation observer failed: " + exception.getMessage());
            }
        }
    }
}