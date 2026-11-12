package com.hoteluq.domain.model;

import java.util.Objects;

public final class Customer {
    private final String id;
    private final String fullName;
    private final String email;

    public Customer(String id, String fullName, String email) {
        this.id = requireText(id, "Customer id");
        this.fullName = requireText(fullName, "Customer name");
        this.email = requireText(email, "Customer email");
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }

    @Override
    public boolean equals(Object other) {
        return other instanceof Customer customer && id.equals(customer.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}