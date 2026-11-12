package com.hoteluq.application.port;

import com.hoteluq.infrastructure.payment.PaymentMovement;

import java.util.List;

public interface PaymentMovementRepository {
    PaymentMovement save(PaymentMovement movement);
    List<PaymentMovement> findByCardId(String cardId);
    List<PaymentMovement> findAll();
}
