package com.hoteluq.application.port;

import com.hoteluq.domain.model.PaymentMovement;

import java.util.List;
import java.util.Optional;

public interface PaymentMovementRepository {
    PaymentMovement save(PaymentMovement movement);
    List<PaymentMovement> findByCardId(String cardId);
    List<PaymentMovement> findByRelatedChargeId(String relatedChargeId);
    Optional<PaymentMovement> findByIdempotencyKey(String idempotencyKey);
    List<PaymentMovement> findAll();
}
