package com.hoteluq.application.promotion;

import com.hoteluq.application.reservation.ReservationEvent;
import com.hoteluq.application.reservation.ReservationEventObserver;
import com.hoteluq.domain.model.Reservation;

/** Observer: when a reservation is created (paid), the voucher it used is consumed. */
public final class VoucherUsageObserver implements ReservationEventObserver {
    private final PromotionService promotionService;

    public VoucherUsageObserver(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event.getType() != ReservationEvent.Type.CREATED) return;
        Reservation reservation = event.getReservation();
        promotionService.confirmVoucherUse(reservation.getCustomer(), reservation.getRoom(),
                reservation.getCheckInAt(), reservation.getCheckOutAt());
    }
}