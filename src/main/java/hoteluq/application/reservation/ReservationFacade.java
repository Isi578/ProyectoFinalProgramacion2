package com.hoteluq.application.reservation;

import com.hoteluq.domain.model.Reservation;
import com.hoteluq.domain.model.Room;

import java.time.LocalDateTime;
import java.util.List;

public final class ReservationFacade implements ReservationUseCase {
    private final ReservationUseCase reservationService;

    public ReservationFacade(ReservationUseCase reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public List<Room> findAvailableRooms(LocalDateTime checkInAt, LocalDateTime checkOutAt,
                                         int guestCount) {
        return reservationService.findAvailableRooms(checkInAt, checkOutAt, guestCount);
    }

    @Override
    public Reservation createReservation(ReservationRequest request) {
        return reservationService.createReservation(request);
    }

    @Override
    public CancellationResult cancelReservation(
            String reservationId, LocalDateTime cancelledAt) {
        return reservationService.cancelReservation(reservationId, cancelledAt);
    }

    @Override
    public List<Reservation> getGuestReservations(String customerId) {
        return reservationService.getGuestReservations(customerId);
    }

    @Override
    public List<ReservationEvent> getActivityHistory() {
        return reservationService.getActivityHistory();
    }
}
