package com.hoteluq.application.reservation;

import com.hoteluq.application.contract.HotelReservationAdministrationUseCase;
import com.hoteluq.application.port.CustomerCatalog;
import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.PromotionPolicy;
import com.hoteluq.application.port.ReservationRepository;
import com.hoteluq.application.port.RoomCatalog;
import com.hoteluq.application.port.ServiceCatalog;
import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.PaymentReceipt;
import com.hoteluq.domain.model.Reservation;
import com.hoteluq.domain.model.ReservationStatus;
import com.hoteluq.domain.model.Room;
import com.hoteluq.domain.model.ServiceOption;
import com.hoteluq.application.contract.LoyaltyPointsUseCase;
import com.hoteluq.application.loyalty.PointsPaymentPolicy;
import com.hoteluq.domain.pricing.AccommodationPricingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ReservationApplicationService implements ReservationUseCase,
        HotelReservationAdministrationUseCase {
    private final RoomCatalog roomCatalog;
    private final ReservationRepository reservationRepository;
    private final ServiceCatalog serviceCatalog;
    private final CustomerCatalog customerCatalog;
    private final PaymentGateway paymentGateway;
    private final PromotionPolicy promotionPolicy;
    private final AccommodationPricingService pricingService;
    private final AvailabilityService availabilityService;
    private final CancellationPolicy guestCancellationPolicy;
    private final CancellationPolicy hotelCancellationPolicy;
    private final ReservationEventPublisher eventPublisher;
    private final ActivityHistoryObserver historyObserver;
    private final LoyaltyPointsUseCase loyalty;

    public ReservationApplicationService(
            RoomCatalog roomCatalog,
            ReservationRepository reservationRepository,
            ServiceCatalog serviceCatalog,
            CustomerCatalog customerCatalog,
            PaymentGateway paymentGateway,
            PromotionPolicy promotionPolicy,
            AccommodationPricingService pricingService,
            CancellationPolicy guestCancellationPolicy,
            CancellationPolicy hotelCancellationPolicy,
            ReservationEventPublisher eventPublisher,
            ActivityHistoryObserver historyObserver) {
        this(roomCatalog, reservationRepository, serviceCatalog, customerCatalog,
                paymentGateway, promotionPolicy, pricingService, guestCancellationPolicy,
                hotelCancellationPolicy, eventPublisher, historyObserver, null);
    }

    /** Permite utilizar puntos dentro de la reserva, antes de realizar el cargo. */
    public ReservationApplicationService(
            RoomCatalog roomCatalog, ReservationRepository reservationRepository,
            ServiceCatalog serviceCatalog, CustomerCatalog customerCatalog,
            PaymentGateway paymentGateway, PromotionPolicy promotionPolicy,
            AccommodationPricingService pricingService,
            CancellationPolicy guestCancellationPolicy, CancellationPolicy hotelCancellationPolicy,
            ReservationEventPublisher eventPublisher, ActivityHistoryObserver historyObserver,
            LoyaltyPointsUseCase loyalty) {
        this.loyalty = loyalty;
        this.roomCatalog = roomCatalog;
        this.reservationRepository = reservationRepository;
        this.serviceCatalog = serviceCatalog;
        this.customerCatalog = customerCatalog;
        this.paymentGateway = paymentGateway;
        this.promotionPolicy = promotionPolicy;
        this.pricingService = pricingService;
        this.availabilityService = new AvailabilityService(roomCatalog, reservationRepository);
        this.guestCancellationPolicy = guestCancellationPolicy;
        this.hotelCancellationPolicy = hotelCancellationPolicy;
        this.eventPublisher = eventPublisher;
        this.historyObserver = historyObserver;
    }

    @Override
    public List<Room> findAvailableRooms(LocalDateTime checkInAt, LocalDateTime checkOutAt,
                                         int guestCount) {
        return availabilityService.findAvailableRooms(checkInAt, checkOutAt, guestCount);
    }

    /**
     * Cotización sin efectos laterales: revisa habitación, fechas, servicios,
     * promociones y disponibilidad de puntos antes de crear la reserva.
     */
    @Override
    public synchronized ReservationQuote quoteReservation(ReservationRequest request) {
        validateRequest(request, false);
        ReservationCalculation data = calculateReservation(request);
        return data.quote();
    }

    @Override
    public synchronized Reservation createReservation(ReservationRequest request) {
        validateRequest(request, true);
        ReservationCalculation data = calculateReservation(request);
        String operationId = request.getPaymentOperationId().trim();
        int points = request.getPointsToSpend();
        boolean spent = false;
        PaymentReceipt payment;
        try {
            if (points > 0) {
                loyalty.spendForPayment(data.customer().getId(), points, operationId);
                spent = true;
            }
            payment = paymentGateway.charge(operationId,
                    paymentOperationReference(request, data.customer().getId(), data.room(),
                            data.services(), data.quote().cardDue()),
                    request.getCardId().trim(), data.customer().getId(), data.quote().cardDue());
        } catch (RuntimeException failure) {
            if (spent) loyalty.restorePayment(data.customer().getId(), points, operationId);
            throw failure;
        }

        Reservation reservation = new Reservation(data.customer(), data.room(),
                request.getCheckInAt(), request.getCheckOutAt(), request.getGuestCount(),
                data.services(), data.quote().cardDue(), payment, points,
                data.quote().pointsValue());
        reservation.confirm();
        try {
            reservationRepository.save(reservation);
        } catch (RuntimeException saveFailure) {
            try {
                paymentGateway.refund("reservation-compensation:" + operationId,
                        payment, payment.getAmount());
            } catch (RuntimeException refundFailure) {
                saveFailure.addSuppressed(refundFailure);
            }
            if (spent) loyalty.restorePayment(data.customer().getId(), points, operationId);
            throw new ReservationException(
                    "No se pudo guardar la reserva; se intentó devolver el pago.", saveFailure);
        }
        publish(ReservationEvent.Type.CREATED, reservation);
        return reservation;
    }

    /** Datos ya validados reutilizados por la cotización y la confirmación. */
    private record ReservationCalculation(Customer customer, Room room,
                                          List<ServiceOption> services,
                                          ReservationQuote quote) { }

    private ReservationCalculation calculateReservation(ReservationRequest request) {
        AvailabilityService.validateStay(request.getCheckInAt(), request.getCheckOutAt());

        if (request.getCheckInAt().isBefore(LocalDateTime.now())) {
            throw new InvalidReservationException("Check-in cannot be in the past.");
        }

        Customer customer = customerCatalog.findById(request.getCustomerId().trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer not found: " + request.getCustomerId()));

        Room room = roomCatalog.findActiveRoom(request.getRoomNumber().trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Active room not found: " + request.getRoomNumber()));

        if (request.getGuestCount() > room.getCapacity()) {
            throw new InvalidReservationException(
                    "Guest count exceeds room capacity of " + room.getCapacity() + ".");
        }

        if (!availabilityService.isAvailable(room, request.getCheckInAt(), request.getCheckOutAt())) {
            throw new RoomUnavailableException("Room " + room.getNumber()
                    + " is not available for the selected dates.");
        }

        List<ServiceOption> selectedServices = resolveServices(request.getServiceIds());
        long nights = ChronoUnit.DAYS.between(
                request.getCheckInAt().toLocalDate(),
                request.getCheckOutAt().toLocalDate());
        if (nights < 1) {
            throw new InvalidReservationException("A reservation must include at least one night.");
        }

        BigDecimal subtotal = pricingService.calculate(
                room, nights, request.getGuestCount(), selectedServices);
        BigDecimal total = promotionPolicy.apply(
                request.getPromotionCode(), customer, room,
                request.getCheckInAt(), request.getCheckOutAt(), subtotal);

        if (total == null || total.signum() <= 0 || total.compareTo(subtotal) > 0) {
            throw new InvalidReservationException("Promotion produced an invalid reservation total.");
        }

        int points = request.getPointsToSpend();
        if (points < 0) throw new InvalidReservationException("Los puntos no pueden ser negativos.");
        if (points > 0 && loyalty == null) {
            throw new InvalidReservationException("El pago con puntos no está habilitado.");
        }
        if (points > 0 && loyalty.getAvailablePoints(customer.getId()) < points) {
            throw new InvalidReservationException("Puntos insuficientes para la reserva.");
        }
        BigDecimal cardDue = PointsPaymentPolicy.remainder(total, points);
        // Las reservas solo admiten pago parcial con puntos: siempre queda un cargo real.
        if (cardDue.signum() <= 0) {
            throw new InvalidReservationException(
                    "En reservas debes pagar al menos una parte con tarjeta.");
        }
        return new ReservationCalculation(customer, room, selectedServices,
                new ReservationQuote(subtotal, total, points,
                        PointsPaymentPolicy.discount(points), cardDue));
    }

    @Override
    public synchronized CancellationResult cancelReservation(
            String reservationId, LocalDateTime cancelledAt) {
        return cancel(reservationId, false, cancelledAt);
    }

    @Override
    public synchronized CancellationResult cancelByHotel(
            String reservationId, LocalDateTime cancelledAt) {
        return cancel(reservationId, true, cancelledAt);
    }

    private CancellationResult cancel(
            String reservationId, boolean hotelInitiated, LocalDateTime cancelledAt) {
        if (reservationId == null || reservationId.isBlank()) {
            throw new InvalidReservationException("Reservation id is required.");
        }
        if (cancelledAt == null) {
            throw new InvalidReservationException("Cancellation time is required.");
        }

        Reservation reservation = reservationRepository.findById(reservationId.trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reservation not found: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new CancellationNotAllowedException(
                    "Only confirmed reservations can be cancelled.");
        }
        if (!hotelInitiated && cancelledAt.isAfter(reservation.getCheckInAt())) {
            throw new CancellationNotAllowedException(
                    "A guest cannot cancel after check-in has started.");
        }

        CancellationPolicy policy = hotelInitiated
                ? hotelCancellationPolicy : guestCancellationPolicy;
        RefundDecision decision = policy.calculate(
                cancelledAt, reservation.getCheckInAt(), reservation.getTotalPaid());

        if (decision.getRefundAmount().signum() > 0) {
            paymentGateway.refund(
                    "reservation-cancellation:" + reservation.getId(),
                    reservation.getPaymentReceipt(), decision.getRefundAmount());
        }

        // Los puntos devueltos siguen la misma proporción de la política de cancelación.
        // La devolución del hotel es completa; la del huésped respeta el porcentaje.
        if (reservation.getPointsUsed() > 0 && loyalty != null) {
            java.math.BigDecimal fraction = decision.getRefundAmount()
                    .divide(reservation.getTotalPaid(), 10, java.math.RoundingMode.HALF_UP);
            int restored = java.math.BigDecimal.valueOf(reservation.getPointsUsed())
                    .multiply(fraction)
                    .setScale(0, java.math.RoundingMode.DOWN).intValue();
            if (restored > 0) loyalty.restorePayment(
                    reservation.getCustomer().getId(), restored,
                    "CANCELACION-" + reservation.getId());
        }
        reservation.cancel();
        reservationRepository.save(reservation);
        publish(ReservationEvent.Type.CANCELLED, reservation);
        return new CancellationResult(
                reservation, decision.getRefundAmount(), decision.getReason());
    }

    @Override
    public List<Reservation> getGuestReservations(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidReservationException("Customer id is required.");
        }
        return reservationRepository.findByCustomerId(customerId.trim());
    }

    @Override
    public List<ReservationEvent> getActivityHistory() {
        return historyObserver.getEvents();
    }

    private List<ServiceOption> resolveServices(List<String> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) return List.of();
        Set<String> uniqueIds = new HashSet<>();
        List<ServiceOption> result = new ArrayList<>();

        for (String rawId : serviceIds) {
            if (rawId == null || rawId.isBlank()) {
                throw new InvalidReservationException("Service ids cannot be blank.");
            }
            String id = rawId.trim();
            if (!uniqueIds.add(id)) {
                throw new InvalidReservationException("A service cannot be selected more than once: " + id);
            }
            ServiceOption service = serviceCatalog.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Service not found: " + id));
            if (!service.isAvailable()) {
                throw new InvalidReservationException("Service is unavailable: " + service.getName());
            }
            result.add(service);
        }
        return List.copyOf(result);
    }

    private void validateRequest(ReservationRequest request, boolean paymentRequired) {
        if (request == null) throw new InvalidReservationException("Reservation request is required.");
        requireText(request.getCustomerId(), "Customer id");
        requireText(request.getRoomNumber(), "Room number");
        requireText(request.getCardId(), "Card id");
        if (paymentRequired) requireText(request.getPaymentOperationId(), "Payment operation id");
        if (request.getGuestCount() < 1) {
            throw new InvalidReservationException("At least one guest is required.");
        }
    }

    private void publish(ReservationEvent.Type type, Reservation reservation) {
        eventPublisher.publish(new ReservationEvent(type, reservation, LocalDateTime.now()));
    }

    private static String paymentOperationReference(
            ReservationRequest request, String customerId, Room room,
            List<ServiceOption> selectedServices, BigDecimal total) {
        StringBuilder reference = new StringBuilder();
        appendReferencePart(reference, customerId);
        appendReferencePart(reference, room.getNumber());
        appendReferencePart(reference, request.getCheckInAt().toString());
        appendReferencePart(reference, request.getCheckOutAt().toString());
        appendReferencePart(reference, Integer.toString(request.getGuestCount()));
        appendReferencePart(reference, request.getCardId().trim());
        appendReferencePart(reference, request.getPromotionCode() == null
                ? "" : request.getPromotionCode().trim());
        appendReferencePart(reference, total.stripTrailingZeros().toPlainString());
        appendReferencePart(reference, Integer.toString(request.getPointsToSpend()));
        for (ServiceOption service : selectedServices) {
            appendReferencePart(reference, service.getId());
        }
        return reference.toString();
    }

    private static void appendReferencePart(StringBuilder reference, String value) {
        reference.append(value.length()).append(':').append(value);
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new InvalidReservationException(label + " is required.");
        }
    }
}
