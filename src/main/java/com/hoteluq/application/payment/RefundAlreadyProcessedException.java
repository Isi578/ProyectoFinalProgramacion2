package com.hoteluq.application.payment;

public final class RefundAlreadyProcessedException extends PaymentDeclinedException {
    public RefundAlreadyProcessedException(String message) { super(message); }
}