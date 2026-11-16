package com.hoteluq.infrastructure.payment;

import com.hoteluq.application.payment.PaymentDeclinedException;
import com.hoteluq.application.port.VirtualCardPaymentPort;
import com.hoteluq.domain.model.PaymentReceipt;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/** Stateless demo integration port. PaymentService owns financial state and idempotency. */
public final class InMemoryVirtualCardPaymentPort implements VirtualCardPaymentPort {
    @Override
    public PaymentReceipt charge(String cardId, String customerId, BigDecimal amount) {
        requireText(cardId, "Card id");
        requireText(customerId, "Customer id");
        validatePositiveAmount(amount);
        return new PaymentReceipt(
                UUID.randomUUID().toString(), cardId.trim(), amount, LocalDateTime.now());
    }

    @Override
    public PaymentReceipt charge(String idempotencyKey, String cardId,
                                 String customerId, BigDecimal amount) {
        requireText(idempotencyKey, "Idempotency key");
        requireText(cardId, "Card id");
        requireText(customerId, "Customer id");
        validatePositiveAmount(amount);
        String transactionId = UUID.nameUUIDFromBytes(
                ("hotel-uq-charge:" + idempotencyKey.trim()).getBytes(StandardCharsets.UTF_8))
                .toString();
        return new PaymentReceipt(transactionId, cardId.trim(), amount, LocalDateTime.now());
    }

    @Override
    public void recharge(String cardId, BigDecimal amount) {
        requireText(cardId, "Card id");
        validatePositiveAmount(amount);
    }

    @Override
    public void recharge(String idempotencyKey, String cardId, BigDecimal amount) {
        requireText(idempotencyKey, "Idempotency key");
        recharge(cardId, amount);
    }

    @Override
    public void refund(PaymentReceipt originalPayment, BigDecimal refundAmount) {
        if (originalPayment == null) {
            throw new PaymentDeclinedException("Original payment is required for a refund.");
        }
        validatePositiveAmount(refundAmount);
    }

    @Override
    public void refund(String idempotencyKey, PaymentReceipt originalPayment,
                       BigDecimal refundAmount) {
        requireText(idempotencyKey, "Idempotency key");
        refund(originalPayment, refundAmount);
    }

    private static void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentDeclinedException("Amount must be greater than zero.");
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new PaymentDeclinedException(label + " is required.");
        }
    }
}
