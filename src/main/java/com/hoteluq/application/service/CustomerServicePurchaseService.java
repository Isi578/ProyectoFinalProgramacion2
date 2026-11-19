package com.hoteluq.application.service;

import com.hoteluq.application.contract.CustomerServicePurchaseUseCase;
import com.hoteluq.application.contract.CustomerVirtualCardUseCase;
import com.hoteluq.application.contract.LoyaltyPointsUseCase;
import com.hoteluq.application.loyalty.PointsPaymentPolicy;
import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.ServiceCatalog;
import com.hoteluq.application.port.ServicePurchaseRepository;
import com.hoteluq.domain.model.PaymentReceipt;
import com.hoteluq.domain.model.ServiceOption;
import com.hoteluq.domain.model.ServicePurchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Compras de servicios independientes de una reserva.
 *
 * Agrupa varios servicios en una sola compra, aplica los puntos de forma
 * voluntaria y registra también las compras pagadas totalmente con puntos.
 * El saldo y la titularidad de la tarjeta se verifican antes de gastar puntos.
 */
public final class CustomerServicePurchaseService implements CustomerServicePurchaseUseCase {
    private final ServiceCatalog services;
    private final PaymentGateway payments;
    private final CustomerVirtualCardUseCase cards;
    private final LoyaltyPointsUseCase loyalty;
    private final ServicePurchaseRepository purchases;

    public CustomerServicePurchaseService(ServiceCatalog services, PaymentGateway payments,
                                          CustomerVirtualCardUseCase cards,
                                          LoyaltyPointsUseCase loyalty,
                                          ServicePurchaseRepository purchases) {
        this.services = services;
        this.payments = payments;
        this.cards = cards;
        this.loyalty = loyalty;
        this.purchases = purchases;
    }

    @Override
    public synchronized PaymentReceipt purchaseServices(String customerId, String cardId,
                                                        List<String> serviceIds, long nights,
                                                        int guestCount, int pointsToSpend) {
        requireText(customerId, "Cliente");
        requireText(cardId, "Tarjeta");
        if (nights < 1 || guestCount < 1) {
            throw new IllegalArgumentException("Las noches y los huéspedes deben ser positivos.");
        }
        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un servicio.");
        }
        boolean ownsActiveCard = cards.findCardsForCustomer(customerId).stream()
                .anyMatch(card -> card.isActive() && card.getCardId().equals(cardId));
        if (!ownsActiveCard) {
            throw new IllegalArgumentException("Selecciona una tarjeta activa de tu cuenta.");
        }
        Set<String> seen = new HashSet<>();
        List<ServiceOption> selected = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (String serviceId : serviceIds) {
            requireText(serviceId, "Servicio");
            if (!seen.add(serviceId.trim())) {
                throw new IllegalArgumentException("No repitas el mismo servicio.");
            }
            ServiceOption service = services.findById(serviceId.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado."));
            if (!service.isAvailable()) {
                throw new IllegalArgumentException("El servicio no está disponible.");
            }
            selected.add(service);
            total = total.add(service.priceFor(nights, guestCount));
        }
        BigDecimal cardDue = PointsPaymentPolicy.remainder(total, pointsToSpend);
        if (pointsToSpend > loyalty.getAvailablePoints(customerId)) {
            throw new IllegalArgumentException("No tienes suficientes puntos.");
        }
        if (cardDue.signum() > 0 && cards.getCustomerCardBalance(customerId, cardId)
                .compareTo(cardDue) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente en la tarjeta.");
        }
        String operationId = "COMPRA-" + UUID.randomUUID();
        boolean spent = false;
        PaymentReceipt receipt = null;
        try {
            if (pointsToSpend > 0) {
                loyalty.spendForPayment(customerId, pointsToSpend, operationId);
                spent = true;
            }
            if (cardDue.signum() > 0) {
                String reference = "COMPRA-SERVICIOS:" + String.join(",", seen);
                receipt = payments.charge(operationId, reference, cardId, customerId, cardDue);
            } else {
                // No se registra un cargo ficticio de $0 en movimientos fiscales.
                receipt = new PaymentReceipt("SOLO-PUNTOS-" + operationId,
                        cardId, BigDecimal.ZERO, LocalDateTime.now());
            }
            purchases.save(new ServicePurchase(operationId, customerId,
                    selected.stream().map(ServiceOption::getId).toList(),
                    selected.stream().map(ServiceOption::getName).toList(),
                    total, cardDue, pointsToSpend, receipt.getPaidAt()));
        } catch (RuntimeException problem) {
            // Si falló guardar la compra después del cargo, reembolsamos
            // el dinero y restituimos los puntos sin dejar un cobro huérfano.
            if (receipt != null && receipt.getAmount().signum() > 0) {
                try {
                    payments.refund("DEVOLUCION-" + operationId,
                            receipt, receipt.getAmount());
                } catch (RuntimeException refundFailure) {
                    problem.addSuppressed(refundFailure);
                }
            }
            if (spent) loyalty.restorePayment(customerId, pointsToSpend, operationId);
            throw problem;
        }
        return receipt;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " obligatorio.");
        }
    }
}
