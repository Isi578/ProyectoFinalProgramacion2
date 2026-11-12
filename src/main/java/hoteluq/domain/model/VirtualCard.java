package com.hoteluq.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class VirtualCard {
    private final String cardId;
    private final String customerId;
    private BigDecimal balance;
    private boolean active;
    private final LocalDateTime createdAt;

    public VirtualCard(String cardId, String customerId, BigDecimal balance,
                       boolean active, LocalDateTime createdAt) {
        this.cardId = cardId;
        this.customerId = customerId;
        this.balance = balance;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getCardId() { return cardId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
