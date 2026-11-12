package com.hoteluq.infrastructure.memory;

import com.hoteluq.application.port.PaymentMovementRepository;
import com.hoteluq.domain.model.PaymentMovement;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryPaymentMovementRepository implements PaymentMovementRepository {
    private final ConcurrentMap<String, PaymentMovement> movements = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PaymentMovement> movementsByIdempotencyKey =
            new ConcurrentHashMap<>();

    @Override
    public PaymentMovement save(PaymentMovement movement) {
        movements.put(movement.getId(), movement);
        if (movement.getIdempotencyKey() != null) {
            movementsByIdempotencyKey.putIfAbsent(movement.getIdempotencyKey(), movement);
        }
        return movement;
    }

    @Override
    public List<PaymentMovement> findByCardId(String cardId) {
        return movements.values().stream()
                .filter(movement -> movement.getCardId().equals(cardId))
                .toList();
    }

    @Override
    public List<PaymentMovement> findByRelatedChargeId(String relatedChargeId) {
        return movements.values().stream()
                .filter(movement -> relatedChargeId.equals(movement.getRelatedChargeId()))
                .toList();
    }

    @Override
    public Optional<PaymentMovement> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(movementsByIdempotencyKey.get(idempotencyKey));
    }

    @Override
    public List<PaymentMovement> findAll() {
        return List.copyOf(movements.values());
    }
}
