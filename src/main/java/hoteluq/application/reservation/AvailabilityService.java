package com.hoteluq.application.reservation;

import com.hoteluq.application.port.ReservationRepository;
import com.hoteluq.application.port.RoomCatalog;
import com.hoteluq.domain.model.Reservation;
import com.hoteluq.domain.model.ReservationStatus;
import com.hoteluq.domain.model.Room;

import java.time.LocalDateTime;
import java.util.List;

public final class AvailabilityService {
    private final RoomCatalog roomCatalog;
    private final ReservationRepository reservationRepository;

    public AvailabilityService(RoomCatalog roomCatalog, ReservationRepository reservationRepository) {
        this.roomCatalog = roomCatalog;
        this.reservationRepository = reservationRepository;
    }

    public List<Room> findAvailableRooms(LocalDateTime checkInAt, LocalDateTime checkOutAt,
                                         int guestCount) {
        validateStay(checkInAt, checkOutAt);
        if (guestCount < 1) throw new InvalidReservationException("At least one guest is required.");

        return roomCatalog.findActiveRooms().stream()
                .filter(room -> room.getCapacity() >= guestCount)
                .filter(room -> isAvailable(room, checkInAt, checkOutAt))
                .toList();
    }

    public boolean isAvailable(Room room, LocalDateTime checkInAt, LocalDateTime checkOutAt) {
        return reservationRepository.findByRoomNumber(room.getNumber()).stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
                .noneMatch(reservation -> checkInAt.isBefore(reservation.getCheckOutAt())
                        && checkOutAt.isAfter(reservation.getCheckInAt()));
    }

    static void validateStay(LocalDateTime checkInAt, LocalDateTime checkOutAt) {
        if (checkInAt == null || checkOutAt == null) {
            throw new InvalidReservationException("Check-in and check-out are required.");
        }
        if (!checkOutAt.isAfter(checkInAt)) {
            throw new InvalidReservationException("Check-out must be later than check-in.");
        }
    }
}