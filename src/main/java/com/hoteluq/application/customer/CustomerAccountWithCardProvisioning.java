package com.hoteluq.application.customer;

import com.hoteluq.application.contract.CustomerAccountUseCase;
import com.hoteluq.application.contract.CustomerVirtualCardUseCase;
import com.hoteluq.domain.model.Customer;

/** Issues a zero-balance virtual card after a successful customer registration. */
public final class CustomerAccountWithCardProvisioning implements CustomerAccountUseCase {
    private final CustomerAccountUseCase accounts;
    private final CustomerVirtualCardUseCase cards;

    public CustomerAccountWithCardProvisioning(
            CustomerAccountUseCase accounts,
            CustomerVirtualCardUseCase cards) {
        if (accounts == null || cards == null) {
            throw new IllegalArgumentException("Account and card services are required.");
        }
        this.accounts = accounts;
        this.cards = cards;
    }

    @Override
    public Customer register(String fullName, String email, String password) {
        Customer customer = accounts.register(fullName, email, password);
        cards.issueForCustomer(customer.getId());
        return customer;
    }

    @Override
    public Customer authenticate(String email, String password) {
        return accounts.authenticate(email, password);
    }
}
