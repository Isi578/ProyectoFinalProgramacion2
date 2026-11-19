package com.hoteluq.presentation.javafx.reservation;

import com.hoteluq.application.reservation.CancellationResult;
import com.hoteluq.application.reservation.ReservationRequest;
import com.hoteluq.application.reservation.ReservationQuote;
import com.hoteluq.application.contract.LoyaltyPointsUseCase;
import com.hoteluq.application.reservation.ReservationUseCase;
import com.hoteluq.application.port.ServiceCatalog;
import com.hoteluq.application.contract.CustomerVirtualCardUseCase;
import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.Reservation;
import com.hoteluq.domain.model.Room;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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

    // Catálogo compartido: la pantalla muestra los servicios activos actuales.
    private final ServiceCatalog serviceCatalog;

    private final CustomerVirtualCardUseCase customerCards;
    private final LoyaltyPointsUseCase loyalty;

    // ID del cliente con sesión iniciada (null si no hay).
    private String sessionCustomerId;

    private ReservationRequest pendingReservationRequest;

    /**
     * Constructor de ReservationController.
     */
    public ReservationController(
            ReservationView view,
            ReservationUseCase reservationUseCase,
            ServiceCatalog serviceCatalog,
            CustomerVirtualCardUseCase customerCards,
            LoyaltyPointsUseCase loyalty) {

        this.view = view;
        this.reservationUseCase = reservationUseCase;
        this.serviceCatalog = serviceCatalog;
        this.customerCards = customerCards;
        this.loyalty = loyalty;
    }

    /**
     * initialize (inicializar).
     *
     * Asigna una acción a cada botón y deja
     * las reservas bloqueadas hasta iniciar sesión.
     */
    public void initialize() {

        refreshServiceCatalog();

        view.getCheckInDate().valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    refreshServicePrices();
                    view.clearAvailableRooms();
                });

        view.getCheckOutDate().valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    refreshServicePrices();
                    view.clearAvailableRooms();
                });

        view.getGuestCount().valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    refreshServicePrices();
                    view.clearAvailableRooms();
                });

        view.getAvailabilityButton()
                .setOnAction(event -> checkAvailability());

        view.getQuoteButton().setOnAction(event -> quote());
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

        view.setCards(customerCards.findCardsForCustomer(customer.getId()));
        view.showPointsAvailable(loyalty.getAvailablePoints(customer.getId()));

        view.setActionsDisabled(false);

        refreshServiceCatalog();

        show("Bienvenido(a), " + customer.getFullName() + "."
                + LINE + "Selecciona una tarjeta virtual propia para pagar."
                + LINE + "Servicios opcionales: "
                + view.getAvailableServiceNames());
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

            refreshServiceCatalog();

            LocalDateTime checkIn = checkInAt();

            LocalDateTime checkOut = checkOutAt();

            List<Room> rooms = reservationUseCase.findAvailableRooms(
                    checkIn,
                    checkOut,
                    view.getGuestCount().getValue()
            );

            view.setAvailableRooms(rooms);

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

    /** Muestra el precio real calculado por el servicio de reservas, sin cobrar. */
    private void quote() {
        if (!hasSession()) return;
        try {
            ReservationQuote quoted = reservationUseCase.quoteReservation(
                    buildReservationRequest(null));
            view.showPaymentSummary("Subtotal: " + quoted.subtotal().toPlainString()
                    + " COP | Con promoción: " + quoted.afterPromotion().toPlainString()
                    + " COP | Puntos: " + quoted.pointsToSpend() + " ("
                    + quoted.pointsValue().toPlainString() + " COP) | A pagar con tarjeta: "
                    + quoted.cardDue().toPlainString() + " COP");
        } catch (RuntimeException exception) { showError(exception); }
    }

    private void createReservation() {

        if (!hasSession()) {
            return;
        }

        try {

            // Relee el catálogo por si administración lo cambió durante la sesión.
            refreshServiceCatalog();

            ReservationRequest request = buildReservationRequest(null);
            // El mismo cálculo se comprueba de nuevo al cobrar para evitar
            // reservas sin disponibilidad o puntos insuficientes.
            ReservationQuote quoted = reservationUseCase.quoteReservation(request);
            view.showPaymentSummary("Confirmando: " + quoted.cardDue().toPlainString()
                    + " COP con tarjeta y " + quoted.pointsToSpend() + " puntos.");

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
            view.getPointsToSpend().getValueFactory().setValue(0);
            view.showPointsAvailable(loyalty.getAvailablePoints(sessionCustomerId));
            view.showPaymentSummary("Pago confirmado: "
                    + reservation.getTotalPaid().toPlainString() + " COP y "
                    + reservation.getPointsUsed() + " puntos.");

            show("Reserva creada y pagada correctamente."
                    + LINE
                    + ReservationMessages.describeReservation(reservation)
                    + LINE
                    + "Los servicios seleccionados se incluyeron en el cobro.");

        } catch (RuntimeException exception) {

            showError(exception);
        }
    }

    private ReservationRequest buildReservationRequest(
            String paymentOperationId) {

        if (view.getSelectedRoomNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "Consulta disponibilidad y selecciona una habitación primero.");
        }

        // El cliente siempre es el de la sesión, no el del campo de texto.
        ReservationRequest.Builder builder = ReservationRequest.builder()
                .customerId(sessionCustomerId)
                .roomNumber(view.getSelectedRoomNumber())
                .checkInAt(checkInAt())
                .checkOutAt(checkOutAt())
                .guestCount(view.getGuestCount().getValue())
                .serviceIds(view.getSelectedServiceIds())
                .cardId(view.getSelectedCardId())
                .promotionCode(view.getPromotionCode().getText())
                .pointsToSpend(view.getPointsToSpend().getValue());

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
                && first.getPointsToSpend() == second.getPointsToSpend()
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

            view.showPointsAvailable(loyalty.getAvailablePoints(sessionCustomerId));
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

    private void refreshServiceCatalog() {
        long nights = selectedNights();
        int guests = view.getGuestCount().getValue();
        view.setAvailableServices(serviceCatalog.findAll(), nights, guests);
    }

    private void refreshServicePrices() {
        view.refreshServicePrices(selectedNights(), view.getGuestCount().getValue());
    }

    private long selectedNights() {
        if (view.getCheckInDate().getValue() == null
                || view.getCheckOutDate().getValue() == null) {
            return 1;
        }

        long nights = java.time.temporal.ChronoUnit.DAYS.between(
                view.getCheckInDate().getValue(),
                view.getCheckOutDate().getValue());
        return Math.max(1, nights);
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
