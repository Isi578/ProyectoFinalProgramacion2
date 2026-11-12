package com.hoteluq.application.port;

import com.hoteluq.domain.model.PaymentReceipt;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentReceipt charge(String cardId, String customerId, BigDecimal amount);
    PaymentReceipt charge(String idempotencyKey, String cardId, String customerId, BigDecimal amount);
    void recharge(String cardId, BigDecimal amount);
    void refund(PaymentReceipt originalPayment, BigDecimal refundAmount);
}
