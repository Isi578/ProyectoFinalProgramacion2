package com.hoteluq.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PaymentReceipt {
    private final String transactionId;
    private final String cardId;
    private final BigDecimal amount;
    private final LocalDateTime paidAt;

    public PaymentReceipt(String transactionId, String cardId, BigDecimal amount, LocalDateTime paidAt) {
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public String getTransactionId() { return transactionId; }
    public String getCardId() { return cardId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getPaidAt() { return paidAt; }
}