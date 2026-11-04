package com.hoteluq.infrastructure.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PaymentMovement {
    private final String id;
    private final String cardId;
    private final PaymentMovementType type;
    private final BigDecimal amount;
    private final String relatedChargeId;
    private final LocalDateTime occurredAt;

    public PaymentMovement(String id, String cardId, PaymentMovementType type,
                           BigDecimal amount, String relatedChargeId, LocalDateTime occurredAt) {
        this.id = id;
        this.cardId = cardId;
        this.type = type;
        this.amount = amount;
        this.relatedChargeId = relatedChargeId;
        this.occurredAt = occurredAt;
    }

    public String getId() { return id; }
    public String getCardId() { return cardId; }
    public PaymentMovementType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getRelatedChargeId() { return relatedChargeId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
