package com.hoteluq.application.payment;

public final class InactiveCardException extends PaymentDeclinedException {
    public InactiveCardException(String message) { super(message); }
}