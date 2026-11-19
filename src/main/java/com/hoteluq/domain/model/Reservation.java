package com.hoteluq.domain.model;

import com.hoteluq.domain.reservation.state.CancelledState;
import com.hoteluq.domain.reservation.state.ConfirmedState;
import com.hoteluq.domain.reservation.state.PendingPaymentState;
import com.hoteluq.domain.reservation.state.ReservationState;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class Reservation {
    private final String id;
    private final Customer customer;
    private final Room room;
    private final LocalDateTime checkInAt;
    private final LocalDateTime checkOutAt;
    private final int guestCount;
    private final List<ServiceOption> services;
    private final BigDecimal totalPaid;
    // Monto equivalente a los puntos aplicados antes del cobro con tarjeta.
    private final BigDecimal pointsValue;
    private final int pointsUsed;
    private final PaymentReceipt paymentReceipt;
    private ReservationState state;

    public Reservation(Customer customer, Room room, LocalDateTime checkInAt,
                       LocalDateTime checkOutAt, int guestCount,
                       List<ServiceOption> services, BigDecimal totalPaid,
                       PaymentReceipt paymentReceipt) {
        this(customer, room, checkInAt, checkOutAt, guestCount,
                services, totalPaid, paymentReceipt, 0, BigDecimal.ZERO);
    }

    /** Crea una reserva con pago mixto y conserva el valor efectivo del recibo. */
    public Reservation(Customer customer, Room room, LocalDateTime checkInAt,
                       LocalDateTime checkOutAt, int guestCount,
                       List<ServiceOption> services, BigDecimal totalPaid,
                       PaymentReceipt paymentReceipt, int pointsUsed, BigDecimal pointsValue) {
        this.id = UUID.randomUUID().toString();
        this.customer = customer;
        this.room = room;
        this.checkInAt = checkInAt;
        this.checkOutAt = checkOutAt;
        this.guestCount = guestCount;
        this.services = List.copyOf(services);
        this.totalPaid = totalPaid;
        this.pointsUsed = pointsUsed;
        this.pointsValue = pointsValue;
        this.paymentReceipt = paymentReceipt;
        this.state = new PendingPaymentState();
    }

    public void confirm() { transitionTo(new ConfirmedState()); }
    public void cancel() { transitionTo(new CancelledState()); }

    private void transitionTo(ReservationState next) {
        if (!state.canTransitionTo(next.status())) {
            throw new IllegalStateException("Invalid reservation transition: "
                    + state.status() + " -> " + next.status());
        }
        state = next;
    }

    public String getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Room getRoom() { return room; }
    public LocalDateTime getCheckInAt() { return checkInAt; }
    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public int getGuestCount() { return guestCount; }
    public List<ServiceOption> getServices() { return services; }
    /** Dinero efectivamente cobrado a la tarjeta (no incluye puntos). */
    public BigDecimal getTotalPaid() { return totalPaid; }
    public BigDecimal getPointsValue() { return pointsValue; }
    public int getPointsUsed() { return pointsUsed; }
    public BigDecimal getTotalValue() { return totalPaid.add(pointsValue); }
    public PaymentReceipt getPaymentReceipt() { return paymentReceipt; }
    public ReservationStatus getStatus() { return state.status(); }

    @Override
    public String toString() {
        String stateLabel = switch (getStatus()) {
            case PENDING_PAYMENT -> "Pendiente de pago";
            case CONFIRMED -> "Confirmada";
            case CANCELLED -> "Cancelada";
        };
        return "Reserva " + id + " | " + customer.getFullName()
                + " | habitación " + room.getNumber() + " | "
                + checkInAt + " hasta " + checkOutAt + " | "
                + "Tarjeta: " + totalPaid.toPlainString() + " COP | "
                + "Puntos utilizados: " + pointsUsed + " | " + stateLabel;
    }
}
