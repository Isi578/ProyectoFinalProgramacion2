package com.hoteluq.application.port;

import com.hoteluq.domain.model.Reservation;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(String reservationId);
    List<Reservation> findByCustomerId(String customerId);
    List<Reservation> findByRoomNumber(String roomNumber);
    List<Reservation> findAll();
}