package com.hoteluq.infrastructure.memory;

import com.hoteluq.application.port.ServicePurchaseRepository;
import com.hoteluq.domain.model.ServicePurchase;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Repositorio en memoria de compras independientes. */
public final class InMemoryServicePurchaseRepository implements ServicePurchaseRepository {
    private final ConcurrentMap<String, ServicePurchase> purchases = new ConcurrentHashMap<>();

    @Override
    public void save(ServicePurchase purchase) {
        if (purchases.putIfAbsent(purchase.operationId(), purchase) != null) {
            throw new IllegalArgumentException("La compra ya está registrada.");
        }
    }

    @Override
    public List<ServicePurchase> findAll() { return List.copyOf(purchases.values()); }
}
