package hoteluq.application.reservation;

import com.hoteluq.domain.model.ReservationStatus;
import com.hoteluq.presentation.javafx.reservation.ReservationMessages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservationMessagesTest {

    @Test
    void translatesKnownMessages() {
        assertEquals("La tarjeta virtual no tiene saldo suficiente.",
                ReservationMessages.translate("Virtual card has insufficient balance."));
        assertEquals("Solo se pueden cancelar reservas confirmadas.",
                ReservationMessages.translate("Only confirmed reservations can be cancelled."));
    }

    @Test
    void translatesMessagesWithVariableData() {
        assertEquals("La habitación 101 no está disponible en las fechas seleccionadas.",
                ReservationMessages.translate("Room 101 is not available for the selected dates."));
        assertEquals("La cantidad de huéspedes supera la capacidad de la habitación (2).",
                ReservationMessages.translate("Guest count exceeds room capacity of 2."));
        assertEquals("No se encontró la reserva: abc-1",
                ReservationMessages.translate("Reservation not found: abc-1"));
    }

    @Test
    void keepsUnknownMessagesAndHandlesNull() {
        assertEquals("Something new", ReservationMessages.translate("Something new"));
        assertEquals("Ocurrió un error inesperado.", ReservationMessages.translate(null));
        assertEquals("Ocurrió un error inesperado.", ReservationMessages.translate("  "));
    }

    @Test
    void translatesReservationStatus() {
        assertEquals("Confirmada", ReservationMessages.status(ReservationStatus.CONFIRMED));
        assertEquals("Cancelada", ReservationMessages.status(ReservationStatus.CANCELLED));
        assertEquals("Pendiente de pago", ReservationMessages.status(ReservationStatus.PENDING_PAYMENT));
    }
}