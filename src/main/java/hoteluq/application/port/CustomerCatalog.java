package com.hoteluq.application.port;

import com.hoteluq.domain.model.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerCatalog {
    Optional<Customer> findById(String customerId);
    Optional<Customer> findByEmail(String email);
    void save(Customer customer);
    List<Customer> findAll();
}