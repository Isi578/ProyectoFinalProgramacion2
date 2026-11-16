package com.hoteluq.domain.loyalty;

import java.time.LocalDateTime;


public final class PointsLot {
    private final String reservationId;
    private final int earned;
    private int remaining;
    private final LocalDateTime earnedAt;
    private final LocalDateTime expiresAt;

    public PointsLot(String reservationId, int points, LocalDateTime earnedAt) {
        if (reservationId == null || reservationId.isBlank()) {
            throw new LoyaltyException("A points lot needs the reservation that generated it.");
        }
        if (points < 1) throw new LoyaltyException("A points lot must have at least one point.");
        if (earnedAt == null) throw new LoyaltyException("Earned date is required.");
        this.reservationId = reservationId;
        this.earned = points;
        this.remaining = points;
        this.earnedAt = earnedAt;
        this.expiresAt = earnedAt.plusYears(1);
    }

    public boolean isExpired(LocalDateTime now) { return !now.isBefore(expiresAt); }

    /** Points that can still be spent right now (zero when the lot has expired). */
    public int availableAt(LocalDateTime now) { return isExpired(now) ? 0 : remaining; }

    int consume(int amount, LocalDateTime now) {
        int taken = Math.min(amount, availableAt(now));
        remaining -= taken;
        return taken;
    }

    int voidRemaining() {
        int voided = remaining;
        remaining = 0;
        return voided;
    }

    public String getReservationId() { return reservationId; }
    public int getEarned() { return earned; }
    public int getRemaining() { return remaining; }
    public LocalDateTime getEarnedAt() { return earnedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}