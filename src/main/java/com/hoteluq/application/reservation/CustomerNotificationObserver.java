package com.hoteluq.application.reservation;

public final class CustomerNotificationObserver implements ReservationEventObserver {
    @Override
    public void onReservationEvent(ReservationEvent event) {
        System.out.println("[NOTIFICATION] " + event.getType()
                + " sent to " + event.getReservation().getCustomer().getEmail()
                + " for reservation " + event.getReservation().getId());
    }
}