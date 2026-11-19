package com.hoteluq.application.contract;

import com.hoteluq.domain.model.PaymentReceipt;
import java.util.List;

public interface CustomerServicePurchaseUseCase {
    /**
     * Compra varios servicios en una única operación (también permite puntos).
     * El recibo refleja únicamente el dinero cargado a la tarjeta.
     */
    PaymentReceipt purchaseServices(String customerId, String cardId, List<String> serviceIds,
                                    long nights, int guestCount, int pointsToSpend);

    /** Compatibilidad con la opción de compra individual del proyecto original. */
    default PaymentReceipt purchaseService(String customerId, String cardId, String serviceId,
                                           long nights, int guestCount) {
        return purchaseServices(customerId, cardId, List.of(serviceId), nights, guestCount, 0);
    }
}
