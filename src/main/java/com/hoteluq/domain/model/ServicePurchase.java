package com.hoteluq.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Registro de una compra independiente; no crea una reserva de habitación. */
public record ServicePurchase(String operationId, String customerId,
                              List<String> serviceIds, List<String> serviceNames,
                              BigDecimal total, BigDecimal cardPaid, int pointsUsed,
                              LocalDateTime purchasedAt) {
    public ServicePurchase {
        serviceIds = List.copyOf(serviceIds);
        serviceNames = List.copyOf(serviceNames);
    }
}
