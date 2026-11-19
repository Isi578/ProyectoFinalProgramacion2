package com.hoteluq.presentation.javafx.reservation;

import com.hoteluq.application.reservation.ReservationEvent;
import com.hoteluq.domain.model.Reservation;
import com.hoteluq.domain.model.ReservationStatus;
import com.hoteluq.domain.model.Room;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReservationMessages (textos de reservas).
 *
 * Traduce al español los mensajes que llegan
 * desde las capas de aplicación y dominio,
 * y da formato a habitaciones, reservas y eventos.
 *
 * Si un mensaje no está en la lista, se muestra
 * tal como llega, para no ocultar información.
 *
 * No depende de JavaFX, por lo que se puede probar solo.
 */
public final class ReservationMessages {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Mensajes completos conocidos.
    private static final Map<String, String> EXACT =
            new LinkedHashMap<>();

    // Mensajes con datos variables (ID, número, fecha).
    private static final Map<Pattern, String> PATTERNS =
            new LinkedHashMap<>();

    static {

        // Validaciones de reservas.
        exact("At least one guest is required.",
                "Se requiere al menos un huésped.");
        exact("Check-in and check-out are required.",
                "La fecha de entrada y la de salida son obligatorias.");
        exact("Check-out must be later than check-in.",
                "La salida debe ser posterior a la entrada.");
        exact("Check-in cannot be in the past.",
                "La entrada no puede estar en el pasado.");
        exact("A reservation must include at least one night.",
                "La reserva debe incluir al menos una noche.");
        exact("Promotion produced an invalid reservation total.",
                "La promoción generó un total de reserva inválido.");
        exact("Could not save the reservation; "
                        + "a compensating refund was attempted.",
                "No se pudo guardar la reserva; "
                        + "se intentó reembolsar el cobro.");
        exact("Reservation id is required.",
                "El ID de la reserva es obligatorio.");
        exact("Cancellation time is required.",
                "La hora de cancelación es obligatoria.");
        exact("Customer id is required.",
                "El ID del cliente es obligatorio.");
        exact("Room number is required.",
                "El número de habitación es obligatorio.");
        exact("Card id is required.",
                "El ID de la tarjeta es obligatorio.");
        exact("Service ids cannot be blank.",
                "Los IDs de servicio no pueden estar vacíos.");
        exact("Reservation request is required.",
                "La solicitud de reserva es obligatoria.");

        // Cancelaciones.
        exact("Only confirmed reservations can be cancelled.",
                "Solo se pueden cancelar reservas confirmadas.");
        exact("A guest cannot cancel after check-in has started.",
                "El huésped no puede cancelar una vez iniciada la estadía.");
        exact("Cancelled more than 72 hours before arrival.",
                "Cancelada con más de 72 horas de anticipación.");
        exact("Cancelled between 24 and 72 hours before arrival.",
                "Cancelada entre 24 y 72 horas antes de la llegada.");
        exact("Cancelled less than 24 hours before arrival.",
                "Cancelada con menos de 24 horas de anticipación.");
        exact("Reservation cancelled by the hotel.",
                "Reserva cancelada por el hotel.");

        // Pagos.
        exact("Virtual card does not belong to this customer.",
                "La tarjeta virtual no pertenece a este cliente.");
        exact("Virtual card is inactive.",
                "La tarjeta virtual está inactiva.");
        exact("Virtual card has insufficient balance.",
                "La tarjeta virtual no tiene saldo suficiente.");
        exact("Virtual card was not found.",
                "No se encontró la tarjeta virtual.");
        exact("Virtual card has an invalid balance.",
                "La tarjeta virtual tiene un saldo inválido.");
        exact("Amount must be greater than zero.",
                "El monto debe ser mayor que cero.");
        exact("Refund cannot exceed the original payment.",
                "El reembolso no puede superar el pago original.");
        exact("Original payment data does not match the recorded charge.",
                "Los datos del pago original no coinciden con el cobro registrado.");
        exact("Refund exceeds the remaining refundable amount.",
                "El reembolso supera el monto que aún se puede reembolsar.");
        exact("Original payment is required for a refund.",
                "Se requiere el pago original para hacer un reembolso.");
        exact("Charge outcome is unknown; "
                        + "retry with the same idempotency key.",
                "No se pudo confirmar el resultado del cobro; "
                        + "reintenta la operación.");
        exact("Refund outcome is unknown; "
                        + "retry with the same idempotency key.",
                "No se pudo confirmar el resultado del reembolso; "
                        + "reintenta la operación.");
        exact("Gateway returned an invalid charge receipt; "
                        + "retry with the same idempotency key.",
                "La pasarela de pago devolvió un recibo inválido; "
                        + "reintenta la operación.");
        exact("Payment operation could not be registered; "
                        + "no gateway operation was attempted.",
                "No se pudo registrar la operación de pago; "
                        + "no se realizó ningún cobro.");

        // Promociones.
        exact("Promotions are not configured yet; "
                        + "the supplied code cannot be validated.",
                "Las promociones aún no están configuradas; "
                        + "no se puede validar el código ingresado.");
        exact("This voucher belongs to another customer.",
                "Este bono pertenece a otro cliente.");
        exact("This voucher was already used.",
                "Este bono ya fue utilizado.");

        // Mensajes con datos variables.
        pattern("Guest count exceeds room capacity of (\\d+)\\.",
                "La cantidad de huéspedes supera la capacidad "
                        + "de la habitación ($1).");
        pattern("A service cannot be selected more than once: (.+)",
                "Un servicio no se puede seleccionar más de una vez: $1");
        pattern("Service not found: (.+)",
                "No se encontró el servicio: $1");
        pattern("Service is unavailable: (.+)",
                "El servicio no está disponible: $1");
        pattern("Customer not found: (.+)",
                "No se encontró el cliente: $1");
        pattern("Active room not found: (.+)",
                "No se encontró una habitación activa con el número: $1");
        pattern("Reservation not found: (.+)",
                "No se encontró la reserva: $1");
        pattern("Room (.+) is not available for the selected dates\\.",
                "La habitación $1 no está disponible "
                        + "en las fechas seleccionadas.");
        pattern("Unknown promotion code: (.+)",
                "Código de promoción desconocido: $1");
        pattern("Promotion (.+) is not active\\.",
                "La promoción $1 no está activa.");
        pattern("The promotion requires at least (\\d+) nights\\.",
                "La promoción requiere al menos $1 noches.");
        pattern("The promotion is only valid for stays "
                        + "starting between (.+) and (.+)\\.",
                "La promoción solo es válida para estadías que "
                        + "inician entre $1 y $2.");
    }

    private ReservationMessages() {
    }

    /**
     * translate (traducir).
     *
     * Devuelve el mensaje en español si lo conoce;
     * si no, devuelve el original.
     */
    public static String translate(String message) {

        if (message == null || message.isBlank()) {
            return "Ocurrió un error inesperado.";
        }

        String text = message.trim();

        String known = EXACT.get(text);

        if (known != null) {
            return known;
        }

        for (Map.Entry<Pattern, String> rule : PATTERNS.entrySet()) {

            Matcher matcher = rule.getKey().matcher(text);

            if (matcher.matches()) {
                return matcher.replaceFirst(rule.getValue());
            }
        }

        return text;
    }

    /**
     * status (estado de la reserva).
     */
    public static String status(ReservationStatus status) {

        return switch (status) {
            case PENDING_PAYMENT -> "Pendiente de pago";
            case CONFIRMED -> "Confirmada";
            case CANCELLED -> "Cancelada";
        };
    }

    /**
     * describeRoom (describir habitación).
     */
    public static String describeRoom(Room room) {

        return "Habitación " + room.getNumber()
                + " - " + room.getRoomType().getName()
                + " - " + room.getRoomType()
                .getNightlyRate().toPlainString()
                + " COP/noche"
                + " - capacidad " + room.getCapacity();
    }

    /**
     * describeReservation (describir reserva).
     */
    public static String describeReservation(Reservation reservation) {

        return "Reserva " + reservation.getId()
                + " | " + reservation.getCustomer().getFullName()
                + " | habitación " + reservation.getRoom().getNumber()
                + " | " + format(reservation.getCheckInAt())
                + " a " + format(reservation.getCheckOutAt())
                + " | " + reservation.getTotalPaid().toPlainString()
                + " COP con tarjeta | " + reservation.getPointsUsed()
                + " puntos | " + status(reservation.getStatus());
    }

    /**
     * describeEvent (describir evento de actividad).
     */
    public static String describeEvent(ReservationEvent event) {

        String type = switch (event.getType()) {
            case CREATED -> "Creada";
            case CANCELLED -> "Cancelada";
        };

        return format(event.getOccurredAt())
                + " | " + type
                + " | reserva=" + event.getReservation().getId()
                + " | estado=" + status(event.getStatusAtEvent());
    }

    private static String format(LocalDateTime dateTime) {

        return DATE_TIME.format(dateTime);
    }

    private static void exact(String english, String spanish) {

        EXACT.put(english, spanish);
    }

    private static void pattern(String english, String spanish) {

        PATTERNS.put(Pattern.compile(english), spanish);
    }
}
