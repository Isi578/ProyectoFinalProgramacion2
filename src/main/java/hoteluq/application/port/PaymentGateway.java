package com.hoteluq.application.port;

import com.hoteluq.domain.model.PaymentReceipt;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentReceipt charge(String cardId, String customerId, BigDecimal amount);
    PaymentReceipt charge(String idempotencyKey, String cardId, String customerId, BigDecimal amount);
    /** Implementations that invoke a stateful provider must forward this key to that provider. */
    default PaymentReceipt charge(String idempotencyKey, String operationReference,
                                  String cardId, String customerId, BigDecimal amount) {
        return charge(idempotencyKey, cardId, customerId, amount);
    }
    void recharge(String administratorId, String cardId, BigDecimal amount);
    /** Implementations that invoke a stateful provider must make this operation idempotent by key. */
    default void recharge(String idempotencyKey, String administratorId,
                          String cardId, BigDecimal amount) {
        recharge(administratorId, cardId, amount);
    }
    void refund(PaymentReceipt originalPayment, BigDecimal refundAmount);
    void refund(String idempotencyKey, PaymentReceipt originalPayment, BigDecimal refundAmount);
}
