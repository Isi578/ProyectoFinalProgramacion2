package com.hoteluq.application.port;

import com.hoteluq.domain.model.PaymentReceipt;

import java.math.BigDecimal;

/** Narrow integration boundary implemented by the team's virtual-card module. */
public interface VirtualCardPaymentPort {
    PaymentReceipt charge(String cardId, String customerId, BigDecimal amount);
    void refund(PaymentReceipt originalPayment, BigDecimal refundAmount);
}
