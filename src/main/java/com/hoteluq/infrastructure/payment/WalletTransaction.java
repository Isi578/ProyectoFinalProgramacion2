package com.hoteluq.infrastructure.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class WalletTransaction {
    private final String id;
    private final String cardId;
    private final WalletTransactionType type;
    private final BigDecimal amount;
    private final String relatedTransactionId;
    private final LocalDateTime occurredAt;

    public WalletTransaction(String id, String cardId, WalletTransactionType type,
                             BigDecimal amount, String relatedTransactionId,
                             LocalDateTime occurredAt) {
        this.id = id;
        this.cardId = cardId;
        this.type = type;
        this.amount = amount;
        this.relatedTransactionId = relatedTransactionId;
        this.occurredAt = occurredAt;
    }

    public String getId() { return id; }
    public String getCardId() { return cardId; }
    public WalletTransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getRelatedTransactionId() { return relatedTransactionId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}