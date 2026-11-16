package com.hoteluq.application.payment;

public final class InsufficientBalanceException extends PaymentDeclinedException {
    public InsufficientBalanceException(String message) { super(message); }
}