package com.hoteluq.infrastructure.memory;

import com.hoteluq.application.port.VirtualCardRepository;
import com.hoteluq.domain.model.VirtualCard;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryVirtualCardRepository implements VirtualCardRepository {
    private final ConcurrentMap<String, VirtualCard> cards = new ConcurrentHashMap<>();

    @Override
    public VirtualCard save(VirtualCard card) {
        cards.put(card.getCardId(), card);
        return card;
    }

    @Override
    public Optional<VirtualCard> findById(String cardId) {
        return Optional.ofNullable(cards.get(cardId));
    }

    @Override
    public List<VirtualCard> findByCustomerId(String customerId) {
        return cards.values().stream()
                .filter(card -> card.getCustomerId().equals(customerId))
                .toList();
    }

    @Override
    public List<VirtualCard> findAll() {
        return List.copyOf(cards.values());
    }
}
