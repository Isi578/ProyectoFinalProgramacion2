package com.hoteluq.infrastructure.memory;

import com.hoteluq.application.port.CustomerCatalog;
import com.hoteluq.domain.model.Customer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryCustomerCatalog implements CustomerCatalog {
    private final ConcurrentMap<String, Customer> customers = new ConcurrentHashMap<>();

    public InMemoryCustomerCatalog(List<Customer> initialCustomers) {
        if (initialCustomers != null) {
            initialCustomers.forEach(customer -> customers.put(customer.getId(), customer));
        }
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }
}