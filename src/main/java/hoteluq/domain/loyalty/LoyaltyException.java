package com.hoteluq.domain.loyalty;

/** Base domain exception for the loyalty (points) module. */
public class LoyaltyException extends RuntimeException {
    public LoyaltyException(String message) { super(message); }
}