package com.hoteluq.infrastructure.memory;

import com.hoteluq.application.port.CustomerCatalog;
import com.hoteluq.domain.model.Customer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryCustomerCatalog implements CustomerCatalog {
    private final ConcurrentMap<String, Customer> customers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> customerIdsByEmail = new ConcurrentHashMap<>();

    public InMemoryCustomerCatalog(List<Customer> initialCustomers) {
        if (initialCustomers != null) {
            initialCustomers.forEach(this::save);
        }
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String customerId = customerIdsByEmail.get(normalizeEmail(email));
        return customerId == null ? Optional.empty() : findById(customerId);
    }

    @Override
    public void save(Customer customer) {
        if (customer == null) throw new IllegalArgumentException("Customer is required.");
        String emailKey = normalizeEmail(customer.getEmail());
        String existingId = customerIdsByEmail.putIfAbsent(emailKey, customer.getId());
        if (existingId != null && !existingId.equals(customer.getId())) {
            throw new IllegalArgumentException("A customer with this email already exists.");
        }

        Customer existingCustomer = customers.putIfAbsent(customer.getId(), customer);
        if (existingCustomer != null
                && (!existingCustomer.getFullName().equals(customer.getFullName())
                || !existingCustomer.getEmail().equals(customer.getEmail()))) {
            if (existingId == null) customerIdsByEmail.remove(emailKey, customer.getId());
            throw new IllegalArgumentException("A customer with this id already exists.");
        }
    }

    @Override
    public List<Customer> findAll() {
        return List.copyOf(customers.values());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}