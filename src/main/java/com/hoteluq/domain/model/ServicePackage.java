package com.hoteluq.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ServicePackage implements ServiceOption {
    private final String id;
    private final String name;
    private final List<ServiceOption> components;

    private ServicePackage(String id, String name, List<ServiceOption> components) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Package id is required.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Package name is required.");
        if (components == null || components.isEmpty()) throw new IllegalArgumentException("A service package needs at least one item.");
        this.id = id.trim();
        this.name = name.trim();
        this.components = List.copyOf(components);
    }

    public static Builder builder() { return new Builder(); }

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

    /** Builder: assembles a package step by step and validates it before creating it. */
    public static final class Builder {
        private String id;
        private String name;
        private final List<ServiceOption> components = new ArrayList<>();

        private Builder() { }

        public Builder id(String value) { id = value; return this; }
        public Builder name(String value) { name = value; return this; }

        public Builder add(ServiceOption component) {
            if (component == null) throw new IllegalArgumentException("A package component cannot be null.");
            components.add(component);
            return this;
        }

        public Builder addAll(List<ServiceOption> newComponents) {
            newComponents.forEach(this::add);
            return this;
        }

        public ServicePackage build() {
            Set<String> seen = new HashSet<>();
            for (ServiceOption component : components) {
                if (id != null && component.getId().equals(id.trim())) {
                    throw new IllegalArgumentException("A package cannot contain itself.");
                }
                if (!seen.add(component.getId())) {
                    throw new IllegalArgumentException("Duplicated component in package: " + component.getId());
                }
            }
            return new ServicePackage(id, name, components);
        }
    }
}