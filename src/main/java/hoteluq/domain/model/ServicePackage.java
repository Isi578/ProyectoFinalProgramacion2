package com.hoteluq.domain.model;

import java.math.BigDecimal;
import java.util.List;

public final class ServicePackage implements ServiceOption {
    private final String id;
    private final String name;
    private final List<ServiceOption> components;

    public ServicePackage(String id, String name, List<ServiceOption> components) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Package id is required.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Package name is required.");
        if (components == null || components.isEmpty()) throw new IllegalArgumentException("A service package needs at least one item.");
        this.id = id.trim();
        this.name = name.trim();
        this.components = List.copyOf(components);
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }

    @Override
    public boolean isAvailable() {
        return components.stream().allMatch(ServiceOption::isAvailable);
    }

    @Override
    public BigDecimal priceFor(long nights, int guestCount) {
        return components.stream()
                .map(service -> service.priceFor(nights, guestCount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<ServiceOption> getComponents() { return components; }

    @Override public String toString() { return name; }
}