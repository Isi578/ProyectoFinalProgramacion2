package com.hoteluq.application.reservation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ActivityHistoryObserver implements ReservationEventObserver {
    private final List<ReservationEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void onReservationEvent(ReservationEvent event) {
        events.add(event);
    }

    public List<ReservationEvent> getEvents() {
        return List.copyOf(events);
    }
}