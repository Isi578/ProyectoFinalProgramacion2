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
    public synchronized PaymentMovement save(PaymentMovement movement) {
        if (movement == null) throw new IllegalArgumentException("Payment movement is required.");
        PaymentMovement existingByKey = movement.getIdempotencyKey() == null
                ? null : movementsByIdempotencyKey.get(movement.getIdempotencyKey());
        if (existingByKey != null && !isValidOperationUpdate(existingByKey, movement)) {
            throw new IllegalArgumentException("Idempotency key is already recorded for another operation.");
        }
        PaymentMovement existingMovement = movements.get(movement.getId());
        if (existingMovement != null && existingMovement != movement
                && existingMovement != existingByKey) {
            throw new IllegalArgumentException("Payment movement id is already recorded.");
        }
        if (existingByKey != null && !existingByKey.getId().equals(movement.getId())) {
            movements.remove(existingByKey.getId(), existingByKey);
        }
        movements.put(movement.getId(), movement);
        if (movement.getIdempotencyKey() != null) {
            movementsByIdempotencyKey.put(movement.getIdempotencyKey(), movement);
        }
        return movement;
    }

    private static boolean isValidOperationUpdate(
            PaymentMovement previous, PaymentMovement updated) {
        return previous.getType() == updated.getType()
                && previous.getCardId().equals(updated.getCardId())
                && previous.getAmount().compareTo(updated.getAmount()) == 0
                && java.util.Objects.equals(
                        previous.getRelatedChargeId(), updated.getRelatedChargeId())
                && java.util.Objects.equals(
                        previous.getOperationReference(), updated.getOperationReference())
                && updated.getStatus().ordinal() >= previous.getStatus().ordinal();
    }

    @Override
    public synchronized void deleteById(String movementId) {
        PaymentMovement removed = movements.remove(movementId);
        if (removed != null && removed.getIdempotencyKey() != null) {
            movementsByIdempotencyKey.remove(removed.getIdempotencyKey(), removed);
        }
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
