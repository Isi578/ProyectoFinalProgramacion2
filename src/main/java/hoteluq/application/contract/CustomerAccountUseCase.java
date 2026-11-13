package com.hoteluq.application.contract;

import com.hoteluq.domain.model.Customer;

public interface CustomerAccountUseCase {
    Customer register(String fullName, String email, String password);
    Customer authenticate(String email, String password);
}