package com.hoteluq.application.port;

import com.hoteluq.domain.model.PaymentReceipt;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentReceipt charge(String cardId, String customerId, BigDecimal amount, String reservationId);
    void refund(PaymentReceipt originalPayment, BigDecimal refundAmount);
}