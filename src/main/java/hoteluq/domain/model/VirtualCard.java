package com.hoteluq.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public final class VirtualCard {
    private final String cardId;
    private final String customerId;
    private BigDecimal balance;
    private boolean active;
    private final LocalDateTime createdAt;
    private final Set<String> appliedPaymentOperationIds;

    public VirtualCard(String cardId, String customerId, BigDecimal balance,
                       boolean active, LocalDateTime createdAt) {
        this(cardId, customerId, balance, active, createdAt, Set.of());
    }

    private VirtualCard(String cardId, String customerId, BigDecimal balance,
                        boolean active, LocalDateTime createdAt,
                        Set<String> appliedPaymentOperationIds) {
        this.cardId = cardId;
        this.customerId = customerId;
        this.balance = balance;
        this.active = active;
        this.createdAt = createdAt;
        this.appliedPaymentOperationIds = new HashSet<>(appliedPaymentOperationIds);
    }

    public String getCardId() { return cardId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean hasAppliedPaymentOperation(String operationId) {
        return appliedPaymentOperationIds.contains(operationId);
    }

    public VirtualCard withBalanceChange(String operationId, BigDecimal amount) {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("Payment operation id is required.");
        }
        if (amount == null) throw new IllegalArgumentException("Balance change is required.");
        if (hasAppliedPaymentOperation(operationId)) return this;
        Set<String> updatedOperations = new HashSet<>(appliedPaymentOperationIds);
        updatedOperations.add(operationId);
        return new VirtualCard(cardId, customerId, balance.add(amount),
                active, createdAt, updatedOperations);
    }
}
