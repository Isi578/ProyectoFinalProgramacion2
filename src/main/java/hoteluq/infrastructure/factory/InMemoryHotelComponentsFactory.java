package com.hoteluq.infrastructure.factory;

import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.PaymentMovementRepository;
import com.hoteluq.application.port.VirtualCardRepository;
import com.hoteluq.application.payment.PaymentService;
import com.hoteluq.domain.model.AdditionalService;
import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.Room;
import com.hoteluq.domain.model.ServiceOption;
import com.hoteluq.domain.model.ServicePackage;
import com.hoteluq.domain.model.ServicePricingUnit;
import com.hoteluq.domain.model.VirtualCard;
import com.hoteluq.infrastructure.memory.InMemoryCustomerCatalog;
import com.hoteluq.infrastructure.memory.InMemoryPaymentMovementRepository;
import com.hoteluq.infrastructure.memory.InMemoryReservationRepository;
import com.hoteluq.infrastructure.memory.InMemoryRoomCatalog;
import com.hoteluq.infrastructure.memory.InMemoryServiceCatalog;
import com.hoteluq.infrastructure.memory.InMemoryVirtualCardRepository;
import com.hoteluq.infrastructure.payment.InMemoryVirtualCardPaymentPort;
import com.hoteluq.infrastructure.payment.VirtualCardPaymentAdapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class InMemoryHotelComponentsFactory implements HotelComponentsFactory {
    @Override
    public HotelComponents createComponents() {
        RoomFactoryRegistry roomFactories = new RoomFactoryRegistry();
        roomFactories.register("STANDARD", new StandardRoomFactory());
        roomFactories.register("SUITE", new SuiteRoomFactory());

        List<Room> rooms = List.of(
                roomFactories.create("STANDARD", "101", new BigDecimal("180000"), 2),
                roomFactories.create("STANDARD", "102", new BigDecimal("180000"), 3),
                roomFactories.create("SUITE", "201", new BigDecimal("420000"), 4));

        AdditionalService breakfast = new AdditionalService(
                "BREAKFAST", "Breakfast", new BigDecimal("25000"),
                ServicePricingUnit.PER_GUEST_PER_NIGHT, true);
        AdditionalService parking = new AdditionalService(
                "PARKING", "Parking", new BigDecimal("18000"),
                ServicePricingUnit.PER_NIGHT, true);
        AdditionalService spa = new AdditionalService(
                "SPA", "Spa access", new BigDecimal("60000"),
                ServicePricingUnit.PER_STAY, true);
        ServicePackage wellnessPackage = new ServicePackage(
                "WELLNESS", "Wellness package", List.of(spa, breakfast));

        InMemoryVirtualCardPaymentPort cardPayments = new InMemoryVirtualCardPaymentPort();
        cardPayments.registerDemoCard("CARD-ANA-001", "GUEST-ANA-001",
                new BigDecimal("2000000"), true);

        VirtualCardRepository cardRepository = new InMemoryVirtualCardRepository();
        cardRepository.save(new VirtualCard("CARD-ANA-001", "GUEST-ANA-001",
                new BigDecimal("2000000"), true, LocalDateTime.now()));
        PaymentMovementRepository movementRepository = new InMemoryPaymentMovementRepository();
        PaymentGateway cardPaymentGateway = new VirtualCardPaymentAdapter(cardPayments);
        PaymentGateway paymentGateway = new PaymentService(
                cardPaymentGateway, cardRepository, movementRepository);

        return new HotelComponents(
                new InMemoryRoomCatalog(rooms),
                new InMemoryReservationRepository(),
                new InMemoryServiceCatalog(List.of(breakfast, parking, spa, wellnessPackage)),
                new InMemoryCustomerCatalog(List.of(
                        new Customer("GUEST-ANA-001", "Ana Pérez", "ana@example.com"))),
                paymentGateway,
                movementRepository);
    }
}
