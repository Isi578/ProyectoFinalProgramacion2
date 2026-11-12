package com.hoteluq.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PaymentMovement {
    private final String id;
    private final String cardId;
    private final PaymentMovementType type;
    private final BigDecimal amount;
    private final String relatedChargeId;
    private final LocalDateTime occurredAt;
    private final String idempotencyKey;
    private final String operationReference;
    private final PaymentMovementStatus status;

    public PaymentMovement(String id, String cardId, PaymentMovementType type,
                           BigDecimal amount, String relatedChargeId, LocalDateTime occurredAt) {
        this(id, cardId, type, amount, relatedChargeId, occurredAt, null);
    }

    public PaymentMovement(String id, String cardId, PaymentMovementType type,
                           BigDecimal amount, String relatedChargeId, LocalDateTime occurredAt,
                           String idempotencyKey) {
        this(id, cardId, type, amount, relatedChargeId, occurredAt, idempotencyKey, null);
    }

    public PaymentMovement(String id, String cardId, PaymentMovementType type,
                           BigDecimal amount, String relatedChargeId, LocalDateTime occurredAt,
                           String idempotencyKey, String operationReference) {
        this(id, cardId, type, amount, relatedChargeId, occurredAt,
                idempotencyKey, operationReference, PaymentMovementStatus.COMPLETED);
    }

    public PaymentMovement(String id, String cardId, PaymentMovementType type,
                           BigDecimal amount, String relatedChargeId, LocalDateTime occurredAt,
                           String idempotencyKey, String operationReference,
                           PaymentMovementStatus status) {
        this.id = id;
        this.cardId = cardId;
        this.type = type;
        this.amount = amount;
        this.relatedChargeId = relatedChargeId;
        this.occurredAt = occurredAt;
        this.idempotencyKey = idempotencyKey;
        this.operationReference = operationReference;
        this.status = status;
    }

    public String getId() { return id; }
    public String getCardId() { return cardId; }
    public PaymentMovementType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getRelatedChargeId() { return relatedChargeId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getOperationReference() { return operationReference; }
    public PaymentMovementStatus getStatus() { return status; }

    public PaymentMovement withState(
            String movementId, LocalDateTime timestamp, PaymentMovementStatus newStatus) {
        return new PaymentMovement(movementId, cardId, type, amount, relatedChargeId,
                timestamp, idempotencyKey, operationReference, newStatus);
    }
}
