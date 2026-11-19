package com.hoteluq.infrastructure.factory;

import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.PaymentMovementRepository;
import com.hoteluq.application.port.VirtualCardRepository;
import com.hoteluq.application.contract.CardRechargeUseCase;
import com.hoteluq.application.contract.CustomerAccountUseCase;
import com.hoteluq.application.contract.CustomerVirtualCardUseCase;
import com.hoteluq.application.customer.CustomerAccountService;
import com.hoteluq.application.customer.CustomerAccountWithCardProvisioning;
import com.hoteluq.application.payment.PaymentService;
import com.hoteluq.domain.model.AdditionalService;
import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.Room;
import com.hoteluq.domain.model.ServicePackage;
import com.hoteluq.domain.model.ServicePricingUnit;
import com.hoteluq.domain.model.VirtualCard;
import com.hoteluq.infrastructure.memory.InMemoryCustomerCatalog;
import com.hoteluq.infrastructure.memory.InMemoryCustomerCredentialRepository;
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
import java.util.Set;

public final class InMemoryHotelComponentsFactory implements HotelComponentsFactory {
    static final String DEMO_ADMINISTRATOR_ID = "admin";

    @Override
    public HotelComponents createComponents() {
        RoomFactoryRegistry roomFactories = new RoomFactoryRegistry();
        roomFactories.register("STANDARD", new StandardRoomFactory());
        roomFactories.register("SUITE", new SuiteRoomFactory());

        // Un hotel recién iniciado no contiene habitaciones ni servicios ficticios.
        // El administrador puede registrarlos manualmente o cargar el archivo CSV.
        List<Room> rooms = List.of();

        InMemoryVirtualCardPaymentPort cardPayments = new InMemoryVirtualCardPaymentPort();
        VirtualCardRepository cardRepository = new InMemoryVirtualCardRepository();
        PaymentMovementRepository movementRepository = new InMemoryPaymentMovementRepository();
        PaymentGateway cardPaymentGateway = new VirtualCardPaymentAdapter(cardPayments);
        PaymentService paymentService = new PaymentService(
                cardPaymentGateway, cardRepository, movementRepository,
                Set.of(DEMO_ADMINISTRATOR_ID));
        PaymentGateway paymentGateway = paymentService;
        CardRechargeUseCase cardRechargeUseCase = paymentService;
        CustomerVirtualCardUseCase customerVirtualCardUseCase = paymentService;

        // La aplicación ya no carga usuarios ni tarjetas ficticias al iniciar.
        // Los registros de demostración se crean únicamente desde Administración.
        InMemoryCustomerCatalog customerCatalog = new InMemoryCustomerCatalog(List.of());
        InMemoryCustomerCredentialRepository customerCredentialRepository =
                new InMemoryCustomerCredentialRepository();
        CustomerAccountService customerAccountService =
                new CustomerAccountService(customerCatalog, customerCredentialRepository);
        CustomerAccountUseCase customerAccountsWithCards =
                new CustomerAccountWithCardProvisioning(
                        customerAccountService, customerVirtualCardUseCase);
        return new HotelComponents(
                new InMemoryRoomCatalog(rooms),
                new InMemoryReservationRepository(),
                new InMemoryServiceCatalog(List.of()),
                customerCatalog,
                customerAccountsWithCards,
                customerVirtualCardUseCase,
                paymentGateway,
                cardRechargeUseCase,
                movementRepository,
                new com.hoteluq.infrastructure.memory.InMemoryServicePurchaseRepository());
    }
}
