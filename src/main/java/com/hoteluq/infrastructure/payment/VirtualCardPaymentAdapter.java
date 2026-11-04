package com.hoteluq.infrastructure.payment;

import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.domain.model.PaymentReceipt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class VirtualCardPaymentAdapter implements PaymentGateway {
    private final VirtualCardWallet wallet;

    public VirtualCardPaymentAdapter(VirtualCardWallet wallet) {
        this.wallet = wallet;
    }

    @Override
    public PaymentReceipt charge(String cardId, String customerId, BigDecimal amount, String reservationId) {
        WalletTransaction movement = wallet.charge(cardId, customerId, amount);
        return new PaymentReceipt(
                movement.getId(), movement.getCardId(), movement.getAmount(),
                movement.getOccurredAt());
    }

    @Override
    public void refund(PaymentReceipt originalPayment, BigDecimal refundAmount) {
        wallet.refund(originalPayment.getTransactionId(), refundAmount);
    }
}