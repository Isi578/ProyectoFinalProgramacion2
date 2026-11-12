package com.hoteluq.domain.loyalty;

/** Raised when a customer tries to redeem more valid (non-expired) points than they own. */
public final class InsufficientPointsException extends LoyaltyException {
    public InsufficientPointsException(int requested, int available) {
        super("Not enough valid points: requested " + requested + ", available " + available + ".");
    }
}