package com.hoteluq.application.promotion;

import com.hoteluq.application.port.ReservationRepository;
import com.hoteluq.application.reservation.PromotionRejectedException;
import com.hoteluq.domain.model.ReservationStatus;

/** A frequent customer has at least N confirmed reservations before this one. */
public final class FrequentCustomerCondition implements PromotionCondition {
    private final ReservationRepository reservations;
    private final int minConfirmedReservations;

    public FrequentCustomerCondition(ReservationRepository reservations, int minConfirmedReservations) {
        if (reservations == null) throw new IllegalArgumentException("Reservation repository is required.");
        if (minConfirmedReservations < 1) {
            throw new IllegalArgumentException("Minimum reservations must be positive.");
        }
        this.reservations = reservations;
        this.minConfirmedReservations = minConfirmedReservations;
    }

    @Override
    public void check(PromotionContext context) {
        long confirmed = reservations.findByCustomerId(context.customer().getId()).stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
                .count();
        if (confirmed < minConfirmedReservations) {
            throw new PromotionRejectedException("The promotion is for frequent customers (at least "
                    + minConfirmedReservations + " previous confirmed reservations).");
        }
    }
}