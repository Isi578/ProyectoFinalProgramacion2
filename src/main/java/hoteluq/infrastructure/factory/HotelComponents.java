package com.hoteluq.infrastructure.factory;

import com.hoteluq.application.port.CustomerCatalog;
import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.ReservationRepository;
import com.hoteluq.application.port.RoomCatalog;
import com.hoteluq.application.port.ServiceCatalog;

public final class HotelComponents {
    private final RoomCatalog roomCatalog;
    private final ReservationRepository reservationRepository;
    private final ServiceCatalog serviceCatalog;
    private final CustomerCatalog customerCatalog;
    private final PaymentGateway paymentGateway;

    public HotelComponents(RoomCatalog roomCatalog,
                           ReservationRepository reservationRepository,
                           ServiceCatalog serviceCatalog,
                           CustomerCatalog customerCatalog,
                           PaymentGateway paymentGateway) {
        this.roomCatalog = roomCatalog;
        this.reservationRepository = reservationRepository;
        this.serviceCatalog = serviceCatalog;
        this.customerCatalog = customerCatalog;
        this.paymentGateway = paymentGateway;
    }

    public RoomCatalog getRoomCatalog() { return roomCatalog; }
    public ReservationRepository getReservationRepository() { return reservationRepository; }
    public ServiceCatalog getServiceCatalog() { return serviceCatalog; }
    public CustomerCatalog getCustomerCatalog() { return customerCatalog; }
    public PaymentGateway getPaymentGateway() { return paymentGateway; }
}
