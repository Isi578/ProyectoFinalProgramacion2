package com.hoteluq.infrastructure.factory;

import com.hoteluq.application.port.CustomerCatalog;
import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.ReservationRepository;
import com.hoteluq.application.port.RoomCatalog;
import com.hoteluq.application.port.ServiceCatalog;
import com.hoteluq.infrastructure.payment.VirtualCardWallet;

public final class HotelComponents {
    private final RoomCatalog roomCatalog;
    private final ReservationRepository reservationRepository;
    private final ServiceCatalog serviceCatalog;
    private final CustomerCatalog customerCatalog;
    private final PaymentGateway paymentGateway;
    private final VirtualCardWallet virtualCardWallet;

    public HotelComponents(RoomCatalog roomCatalog,
                           ReservationRepository reservationRepository,
                           ServiceCatalog serviceCatalog,
                           CustomerCatalog customerCatalog,
                           PaymentGateway paymentGateway,
                           VirtualCardWallet virtualCardWallet) {
        this.roomCatalog = roomCatalog;
        this.reservationRepository = reservationRepository;
        this.serviceCatalog = serviceCatalog;
        this.customerCatalog = customerCatalog;
        this.paymentGateway = paymentGateway;
        this.virtualCardWallet = virtualCardWallet;
    }

    public RoomCatalog getRoomCatalog() { return roomCatalog; }
    public ReservationRepository getReservationRepository() { return reservationRepository; }
    public ServiceCatalog getServiceCatalog() { return serviceCatalog; }
    public CustomerCatalog getCustomerCatalog() { return customerCatalog; }
    public PaymentGateway getPaymentGateway() { return paymentGateway; }
    public VirtualCardWallet getVirtualCardWallet() { return virtualCardWallet; }
}