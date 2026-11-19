package com.hoteluq.application.service;

import com.hoteluq.application.contract.CustomerServicePurchaseUseCase;
import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.ServiceCatalog;
import com.hoteluq.domain.model.PaymentReceipt;
import com.hoteluq.domain.model.ServiceOption;

import java.math.BigDecimal;
import java.util.UUID;

/** Charges a customer's virtual card for an additional service purchased outside a reservation. */
public final class CustomerServicePurchaseService implements CustomerServicePurchaseUseCase {
    private final ServiceCatalog services;
    private final PaymentGateway payments;

    public CustomerServicePurchaseService(ServiceCatalog services, PaymentGateway payments) {
        if (services == null || payments == null) {
            throw new IllegalArgumentException("Service catalog and payment gateway are required.");
        }
        this.services = services;
        this.payments = payments;
    }

    @Override
    public PaymentReceipt purchaseService(String customerId, String cardId, String serviceId,
                                          long nights, int guestCount) {
        requireText(customerId, "Customer id");
        requireText(cardId, "Card id");
        requireText(serviceId, "Service id");
        if (nights < 1 || guestCount < 1) {
            throw new IllegalArgumentException("Nights and guests must be positive.");
        }

        ServiceOption service = services.findById(serviceId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Service was not found."));
        if (!service.isAvailable()) {
            throw new IllegalArgumentException("This service is currently unavailable.");
        }

        BigDecimal amount = service.priceFor(nights, guestCount);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("The service price must be greater than zero.");
        }

        String operationId = "SERVICE-PURCHASE-" + UUID.randomUUID();
        String reference = "Standalone service purchase: " + service.getId()
                + " | " + nights + " night(s) | " + guestCount + " guest(s)";
        return payments.charge(operationId, reference, cardId.trim(),
                customerId.trim(), amount);
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }
}
