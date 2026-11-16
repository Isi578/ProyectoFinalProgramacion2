package com.hoteluq.application.contract;

import com.hoteluq.application.reservation.CancellationResult;

import java.time.LocalDateTime;

/** Administrative contract for cancellations initiated by HotelUQ. */
public interface HotelReservationAdministrationUseCase {
    CancellationResult cancelByHotel(String reservationId, LocalDateTime cancelledAt);
}
