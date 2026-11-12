package com.hoteluq.infrastructure.memory;

import com.hoteluq.application.port.ServiceCatalog;
import com.hoteluq.domain.model.ServiceOption;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryServiceCatalog implements ServiceCatalog {
    private final ConcurrentMap<String, ServiceOption> services = new ConcurrentHashMap<>();

    public InMemoryServiceCatalog(List<ServiceOption> initialServices) {
        if (initialServices != null) initialServices.forEach(this::add);
    }

    public void add(ServiceOption service) { services.put(service.getId(), service); }

    @Override
    public Optional<ServiceOption> findById(String serviceId) {
        return Optional.ofNullable(services.get(serviceId));
    }

    @Override
    public List<ServiceOption> findAll() {
        return services.values().stream()
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .toList();
    }
}