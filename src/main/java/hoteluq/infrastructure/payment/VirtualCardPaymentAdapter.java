package com.hoteluq.infrastructure.payment;

import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.VirtualCardPaymentPort;
import com.hoteluq.domain.model.PaymentReceipt;

import java.math.BigDecimal;

public final class VirtualCardPaymentAdapter implements PaymentGateway {
    private final VirtualCardPaymentPort cardPayments;

    public VirtualCardPaymentAdapter(VirtualCardPaymentPort cardPayments) {
        this.cardPayments = cardPayments;
    }

    @Override
    public PaymentReceipt charge(String cardId, String customerId, BigDecimal amount) {
        return cardPayments.charge(cardId, customerId, amount);
    }

    @Override
    public PaymentReceipt charge(String idempotencyKey, String cardId,
                                 String customerId, BigDecimal amount) {
        return cardPayments.charge(idempotencyKey, cardId, customerId, amount);
    }

    @Override
    public void recharge(String cardId, BigDecimal amount) {
        cardPayments.recharge(cardId, amount);
    }

    @Override
    public void refund(PaymentReceipt originalPayment, BigDecimal refundAmount) {
        cardPayments.refund(originalPayment, refundAmount);
    }
}
