package com.hoteluq.infrastructure.memory;

import com.hoteluq.application.port.PaymentMovementRepository;
import com.hoteluq.infrastructure.payment.PaymentMovement;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryPaymentMovementRepository implements PaymentMovementRepository {
    private final ConcurrentMap<String, PaymentMovement> movements = new ConcurrentHashMap<>();

    @Override
    public PaymentMovement save(PaymentMovement movement) {
        movements.put(movement.getId(), movement);
        return movement;
    }

    @Override
    public List<PaymentMovement> findByCardId(String cardId) {
        return movements.values().stream()
                .filter(movement -> movement.getCardId().equals(cardId))
                .toList();
    }

    @Override
    public List<PaymentMovement> findAll() {
        return List.copyOf(movements.values());
    }
}
