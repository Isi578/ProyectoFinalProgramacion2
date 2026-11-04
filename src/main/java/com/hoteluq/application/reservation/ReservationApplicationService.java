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

    @Override
    public synchronized Reservation createReservation(ReservationRequest request) {
        validateRequest(request);
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

        PaymentReceipt payment = paymentGateway.charge(
                request.getCardId().trim(), customer.getId(), total, "reservation-pending");

        Reservation reservation = new Reservation(
                customer, room, request.getCheckInAt(), request.getCheckOutAt(),
                request.getGuestCount(), selectedServices, total, payment);
        reservation.confirm();

        try {
            reservationRepository.save(reservation);
        } catch (RuntimeException saveFailure) {
            try {
                paymentGateway.refund(payment, payment.getAmount());
            } catch (RuntimeException refundFailure) {
                saveFailure.addSuppressed(refundFailure);
            }
            throw new ReservationException(
                    "Could not save the reservation; a compensating refund was attempted.", saveFailure);
        }

        publish(ReservationEvent.Type.CREATED, reservation);
        return reservation;
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
            paymentGateway.refund(reservation.getPaymentReceipt(), decision.getRefundAmount());
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

    private void validateRequest(ReservationRequest request) {
        if (request == null) throw new InvalidReservationException("Reservation request is required.");
        requireText(request.getCustomerId(), "Customer id");
        requireText(request.getRoomNumber(), "Room number");
        requireText(request.getCardId(), "Card id");
        if (request.getGuestCount() < 1) {
            throw new InvalidReservationException("At least one guest is required.");
        }
    }

    private void publish(ReservationEvent.Type type, Reservation reservation) {
        eventPublisher.publish(new ReservationEvent(type, reservation, LocalDateTime.now()));
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new InvalidReservationException(label + " is required.");
        }
    }
}
