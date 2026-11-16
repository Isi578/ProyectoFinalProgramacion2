package com.hoteluq.presentation.javafx.reservation;

import com.hoteluq.application.reservation.CancellationResult;
import com.hoteluq.application.reservation.ReservationException;
import com.hoteluq.application.reservation.ReservationRequest;
import com.hoteluq.application.reservation.ReservationUseCase;
import com.hoteluq.domain.model.Reservation;
import com.hoteluq.domain.model.Room;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ReservationController {
    private final ReservationView view;
    private final ReservationUseCase reservationUseCase;
    private ReservationRequest pendingReservationRequest;

    public ReservationController(ReservationView view, ReservationUseCase reservationUseCase) {
        this.view = view;
        this.reservationUseCase = reservationUseCase;
    }

    public void initialize() {
        view.getAvailabilityButton().setOnAction(event -> checkAvailability());
        view.getCreateButton().setOnAction(event -> createReservation());
        view.getCancelButton().setOnAction(event -> cancelReservation());
        view.getHistoryButton().setOnAction(event -> showHistory());
        show("Demo guest: GUEST-ANA-001 | Demo card: CARD-ANA-001"
                + System.lineSeparator() + "Available services: BREAKFAST, PARKING, SPA, WELLNESS");
    }

    private void checkAvailability() {
        try {
            LocalDateTime checkIn = checkInAt();
            LocalDateTime checkOut = checkOutAt();
            List<Room> rooms = reservationUseCase.findAvailableRooms(
                    checkIn, checkOut, view.getGuestCount().getValue());
            if (rooms.isEmpty()) {
                show("No rooms are available for that period and guest count.");
                return;
            }
            StringBuilder result = new StringBuilder("Available rooms:");
            rooms.forEach(room -> result.append(System.lineSeparator()).append(" - ").append(room));
            show(result.toString());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void createReservation() {
        try {
            ReservationRequest request = buildReservationRequest(null);
            if (pendingReservationRequest == null
                    || !hasSameDetails(request, pendingReservationRequest)) {
                pendingReservationRequest = buildReservationRequest(UUID.randomUUID().toString());
            }

            Reservation reservation = reservationUseCase.createReservation(pendingReservationRequest);
            pendingReservationRequest = null;
            view.getReservationId().setText(reservation.getId());
            show("Reservation created and paid successfully."
                    + System.lineSeparator() + reservation
                    + System.lineSeparator() + "To try a promotion, the promotion module must be configured.");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private ReservationRequest buildReservationRequest(String paymentOperationId) {
        ReservationRequest.Builder builder = ReservationRequest.builder()
                .customerId(view.getCustomerId().getText())
                .roomNumber(view.getRoomNumber().getText())
                .checkInAt(checkInAt())
                .checkOutAt(checkOutAt())
                .guestCount(view.getGuestCount().getValue())
                .serviceIds(parseServiceIds(view.getServiceIds().getText()))
                .cardId(view.getCardId().getText())
                .promotionCode(view.getPromotionCode().getText());
        if (paymentOperationId != null) {
            builder.paymentOperationId(paymentOperationId);
        }
        return builder.build();
    }

    private static boolean hasSameDetails(ReservationRequest first, ReservationRequest second) {
        return Objects.equals(first.getCustomerId(), second.getCustomerId())
                && Objects.equals(first.getRoomNumber(), second.getRoomNumber())
                && Objects.equals(first.getCheckInAt(), second.getCheckInAt())
                && Objects.equals(first.getCheckOutAt(), second.getCheckOutAt())
                && first.getGuestCount() == second.getGuestCount()
                && Objects.equals(first.getServiceIds(), second.getServiceIds())
                && Objects.equals(first.getCardId(), second.getCardId())
                && Objects.equals(first.getPromotionCode(), second.getPromotionCode());
    }

    private void cancelReservation() {
        try {
            CancellationResult result = reservationUseCase.cancelReservation(
                    view.getReservationId().getText(), LocalDateTime.now());
            show("Reservation cancelled."
                    + System.lineSeparator() + result.getReservation()
                    + System.lineSeparator() + "Refund: "
                    + result.getRefundAmount().toPlainString() + " COP"
                    + System.lineSeparator() + result.getReason());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void showHistory() {
        try {
            String customerId = view.getCustomerId().getText();
            List<Reservation> reservations = reservationUseCase.getGuestReservations(customerId);
            StringBuilder result = new StringBuilder("Reservation history:");
            if (reservations.isEmpty()) result.append(System.lineSeparator()).append("No reservations found.");
            reservations.forEach(reservation ->
                    result.append(System.lineSeparator()).append(" - ").append(reservation));
            result.append(System.lineSeparator()).append(System.lineSeparator()).append("Activity events:");
            reservationUseCase.getActivityHistory().forEach(event ->
                    result.append(System.lineSeparator()).append(" - ").append(event));
            show(result.toString());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private LocalDateTime checkInAt() {
        if (view.getCheckInDate().getValue() == null) {
            throw new IllegalArgumentException("Choose a check-in date.");
        }
        return view.getCheckInDate().getValue().atTime(LocalTime.parse(view.getCheckInTime().getText().trim()));
    }

    private LocalDateTime checkOutAt() {
        if (view.getCheckOutDate().getValue() == null) {
            throw new IllegalArgumentException("Choose a check-out date.");
        }
        return view.getCheckOutDate().getValue().atTime(LocalTime.parse(view.getCheckOutTime().getText().trim()));
    }

    private static List<String> parseServiceIds(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }

    private void show(String message) {
        view.getOutput().setText(message);
    }

    private void showError(RuntimeException exception) {
        String message = exception instanceof ReservationException
                ? exception.getMessage()
                : "Invalid input: " + exception.getMessage();
        show("Operation failed: " + message);
    }
}
