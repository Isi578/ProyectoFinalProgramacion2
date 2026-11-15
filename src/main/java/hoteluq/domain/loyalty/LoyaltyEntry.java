package com.hoteluq.domain.loyalty;

import java.time.LocalDateTime;


public record LoyaltyEntry(LocalDateTime at, Kind kind, int points, String detail) {
    public enum Kind { EARNED, REDEEMED, REVERTED }

    @Override
    public String toString() {
        return at + " | " + kind + " | " + points + " pts | " + detail;
    }
}