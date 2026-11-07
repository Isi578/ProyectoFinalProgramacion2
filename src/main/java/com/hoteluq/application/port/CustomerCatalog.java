package com.hoteluq.application.port;

import com.hoteluq.domain.model.Customer;

import java.util.Optional;

public interface CustomerCatalog {
    Optional<Customer> findById(String customerId);
}