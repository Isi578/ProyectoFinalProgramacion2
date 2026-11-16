package com.hoteluq.infrastructure.memory;

import com.hoteluq.application.port.CustomerCredentialRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryCustomerCredentialRepository implements CustomerCredentialRepository {
    private final ConcurrentMap<String, Credential> credentials = new ConcurrentHashMap<>();

    @Override
    public void save(String customerId, String salt, String passwordHash) {
        requireText(customerId, "Customer id");
        requireText(salt, "Password salt");
        requireText(passwordHash, "Password hash");
        if (credentials.putIfAbsent(customerId, new Credential(salt, passwordHash)) != null) {
            throw new IllegalArgumentException("Customer credentials already exist.");
        }
    }

    @Override
    public Optional<Credential> findByCustomerId(String customerId) {
        return Optional.ofNullable(credentials.get(customerId));
    }

    @Override
    public void deleteByCustomerId(String customerId) {
        credentials.remove(customerId);
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }
}
