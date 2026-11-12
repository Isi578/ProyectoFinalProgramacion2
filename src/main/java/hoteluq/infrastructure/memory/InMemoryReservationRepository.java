package com.hoteluq.infrastructure.memory;

import com.hoteluq.application.port.ReservationRepository;
import com.hoteluq.domain.model.Reservation;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryReservationRepository implements ReservationRepository {
    private final ConcurrentMap<String, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findById(String reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    @Override
    public List<Reservation> findByCustomerId(String customerId) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getCustomer().getId().equals(customerId))
                .sorted(Comparator.comparing(Reservation::getCheckInAt))
                .toList();
    }

    @Override
    public List<Reservation> findByRoomNumber(String roomNumber) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getRoom().getNumber().equals(roomNumber))
                .toList();
    }

    @Override
    public List<Reservation> findAll() {
        return reservations.values().stream()
                .sorted(Comparator.comparing(Reservation::getCheckInAt))
                .toList();
    }
}