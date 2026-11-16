package com.hoteluq.domain.loyalty;


public final class InsufficientPointsException extends LoyaltyException {
    public InsufficientPointsException(int requested, int available) {
        super("Not enough valid points: requested " + requested + ", available " + available + ".");
    }
}