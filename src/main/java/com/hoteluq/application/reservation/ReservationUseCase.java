package com.hoteluq.application.reservation;

import com.hoteluq.domain.model.Reservation;
import com.hoteluq.domain.model.Room;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationUseCase {
    List<Room> findAvailableRooms(LocalDateTime checkInAt, LocalDateTime checkOutAt, int guestCount);
    Reservation createReservation(ReservationRequest request);
    CancellationResult cancelReservation(String reservationId, LocalDateTime cancelledAt);
    List<Reservation> getGuestReservations(String customerId);
    List<ReservationEvent> getActivityHistory();
}
