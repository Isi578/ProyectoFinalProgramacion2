package com.hoteluq.presentation.javafx.reservation;

import com.hoteluq.application.reservation.CancellationResult;
import com.hoteluq.application.reservation.ReservationRequest;
import com.hoteluq.application.reservation.ReservationUseCase;
import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.Reservation;
import com.hoteluq.domain.model.Room;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * ReservationController (controlador de reservas).
 *
 * Conecta los botones de ReservationView
 * con ReservationUseCase.
 *
 * Solo funciona mientras haya un cliente con sesión
 * iniciada: todas las operaciones se hacen a nombre
 * de ese cliente.
 */
public final class ReservationController {

    private static final String LINE = System.lineSeparator();

    private static final String LOGIN_REQUIRED =
            "Error: debes iniciar sesión para usar las reservas.";

    // Referencia a la vista.
    private final ReservationView view;

    // Contrato de reservas.
    private final ReservationUseCase reservationUseCase;

    // ID del cliente con sesión iniciada (null si no hay).
    private String sessionCustomerId;

    private ReservationRequest pendingReservationRequest;

    /**
     * Constructor de ReservationController.
     */
    public ReservationController(
            ReservationView view,
            ReservationUseCase reservationUseCase) {

        this.view = view;
        this.reservationUseCase = reservationUseCase;
    }

    /**
     * initialize (inicializar).
     *
     * Asigna una acción a cada botón y deja
     * las reservas bloqueadas hasta iniciar sesión.
     */
    public void initialize() {

        view.getAvailabilityButton()
                .setOnAction(event -> checkAvailability());

        view.getCreateButton()
                .setOnAction(event -> createReservation());

        view.getCancelButton()
                .setOnAction(event -> cancelReservation());

        view.getHistoryButton()
                .setOnAction(event -> showHistory());

        endSession();
    }

    /**
     * startSession (iniciar sesión).
     *
     * Habilita las reservas para el cliente indicado.
     */
    public void startSession(Customer customer) {

        sessionCustomerId = customer.getId();

        pendingReservationRequest = null;

        view.clearSession();

        view.showSession(customer.getFullName(), customer.getId());

        view.setActionsDisabled(false);

        show("Bienvenido(a), " + customer.getFullName() + "."
                + LINE + "Tarjeta de demostración: CARD-ANA-001"
                + " (solo para el cliente demo)."
                + LINE + "Servicios disponibles: "
                + "BREAKFAST, PARKING, SPA, WELLNESS");
    }

    /**
     * endSession (cerrar sesión).
     *
     * Bloquea las reservas y borra los datos del cliente.
     */
    public void endSession() {

        sessionCustomerId = null;

        pendingReservationRequest = null;

        view.clearSession();

        view.setActionsDisabled(true);

        show("Inicia sesión en la pestaña «Cuenta de cliente» "
                + "para gestionar tus reservas.");
    }

    private void checkAvailability() {

        if (!hasSession()) {
            return;
        }

        try {

            LocalDateTime checkIn = checkInAt();

            LocalDateTime checkOut = checkOutAt();

            List<Room> rooms = reservationUseCase.findAvailableRooms(
                    checkIn,
                    checkOut,
                    view.getGuestCount().getValue()
            );

            if (rooms.isEmpty()) {

                show("No hay habitaciones disponibles para ese "
                        + "periodo y esa cantidad de huéspedes.");

                return;
            }

            StringBuilder result =
                    new StringBuilder("Habitaciones disponibles:");

            rooms.forEach(room -> result.append(LINE)
                    .append(" - ")
                    .append(ReservationMessages.describeRoom(room)));

            show(result.toString());

        } catch (RuntimeException exception) {

            showError(exception);
        }
    }

    private void createReservation() {

        if (!hasSession()) {
            return;
        }

        try {

            ReservationRequest request = buildReservationRequest(null);

            if (pendingReservationRequest == null
                    || !hasSameDetails(request, pendingReservationRequest)) {

                pendingReservationRequest = buildReservationRequest(
                        UUID.randomUUID().toString()
                );
            }

            Reservation reservation = reservationUseCase
                    .createReservation(pendingReservationRequest);

            pendingReservationRequest = null;

            view.getReservationId().setText(reservation.getId());

            show("Reserva creada y pagada correctamente."
                    + LINE
                    + ReservationMessages.describeReservation(reservation)
                    + LINE
                    + "Para probar una promoción, debe estar "
                    + "configurado el módulo de promociones.");

        } catch (RuntimeException exception) {

            showError(exception);
        }
    }

    private ReservationRequest buildReservationRequest(
            String paymentOperationId) {

        // El cliente siempre es el de la sesión, no el del campo de texto.
        ReservationRequest.Builder builder = ReservationRequest.builder()
                .customerId(sessionCustomerId)
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

    private static boolean hasSameDetails(
            ReservationRequest first,
            ReservationRequest second) {

        return Objects.equals(first.getCustomerId(), second.getCustomerId())
                && Objects.equals(first.getRoomNumber(), second.getRoomNumber())
                && Objects.equals(first.getCheckInAt(), second.getCheckInAt())
                && Objects.equals(first.getCheckOutAt(), second.getCheckOutAt())
                && first.getGuestCount() == second.getGuestCount()
                && Objects.equals(first.getServiceIds(), second.getServiceIds())
                && Objects.equals(first.getCardId(), second.getCardId())
                && Objects.equals(
                first.getPromotionCode(),
                second.getPromotionCode());
    }

    private void cancelReservation() {

        if (!hasSession()) {
            return;
        }

        try {

            String id = view.getReservationId().getText();

            String reservationId = id == null ? "" : id.trim();

            // Solo se pueden cancelar reservas del cliente con sesión.
            boolean isOwnReservation = reservationUseCase
                    .getGuestReservations(sessionCustomerId)
                    .stream()
                    .anyMatch(reservation ->
                            reservation.getId().equals(reservationId));

            if (!isOwnReservation) {

                show("Error: no se encontró esa reserva en tu cuenta.");

                return;
            }

            CancellationResult result = reservationUseCase
                    .cancelReservation(reservationId, LocalDateTime.now());

            show("Reserva cancelada."
                    + LINE
                    + ReservationMessages
                    .describeReservation(result.getReservation())
                    + LINE + "Reembolso: "
                    + result.getRefundAmount().toPlainString() + " COP"
                    + LINE
                    + ReservationMessages.translate(result.getReason()));

        } catch (RuntimeException exception) {

            showError(exception);
        }
    }

    private void showHistory() {

        if (!hasSession()) {
            return;
        }

        try {

            List<Reservation> reservations = reservationUseCase
                    .getGuestReservations(sessionCustomerId);

            StringBuilder result =
                    new StringBuilder("Historial de reservas:");

            if (reservations.isEmpty()) {

                result.append(LINE).append("No se encontraron reservas.");
            }

            reservations.forEach(reservation -> result.append(LINE)
                    .append(" - ")
                    .append(ReservationMessages
                            .describeReservation(reservation)));

            result.append(LINE).append(LINE)
                    .append("Eventos de actividad:");

            // Solo se muestran los eventos del cliente con sesión.
            List<String> events = reservationUseCase.getActivityHistory()
                    .stream()
                    .filter(event -> sessionCustomerId.equals(
                            event.getReservation().getCustomer().getId()))
                    .map(ReservationMessages::describeEvent)
                    .toList();

            if (events.isEmpty()) {

                result.append(LINE).append("Sin eventos registrados.");
            }

            events.forEach(event -> result.append(LINE)
                    .append(" - ")
                    .append(event));

            show(result.toString());

        } catch (RuntimeException exception) {

            showError(exception);
        }
    }

    private LocalDateTime checkInAt() {

        if (view.getCheckInDate().getValue() == null) {

            throw new IllegalArgumentException(
                    "Elige una fecha de entrada."
            );
        }

        return view.getCheckInDate().getValue()
                .atTime(parseTime(view.getCheckInTime().getText()));
    }

    private LocalDateTime checkOutAt() {

        if (view.getCheckOutDate().getValue() == null) {

            throw new IllegalArgumentException(
                    "Elige una fecha de salida."
            );
        }

        return view.getCheckOutDate().getValue()
                .atTime(parseTime(view.getCheckOutTime().getText()));
    }

    private static LocalTime parseTime(String value) {

        try {

            return LocalTime.parse(value == null ? "" : value.trim());

        } catch (DateTimeParseException invalidTime) {

            throw new IllegalArgumentException(
                    "La hora no es válida; usa el formato HH:mm."
            );
        }
    }

    private static List<String> parseServiceIds(String value) {

        if (value == null || value.isBlank()) {

            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }

    /**
     * hasSession (hay sesión).
     *
     * Impide operar si no hay un cliente con sesión iniciada.
     */
    private boolean hasSession() {

        if (sessionCustomerId == null) {

            show(LOGIN_REQUIRED);

            return false;
        }

        return true;
    }

    private void show(String message) {

        view.getOutput().setText(message);
    }

    private void showError(RuntimeException exception) {

        show("No se pudo completar la operación: "
                + ReservationMessages.translate(exception.getMessage()));
    }
}