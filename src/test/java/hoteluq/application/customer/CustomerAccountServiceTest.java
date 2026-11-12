package com.hoteluq.application.customer;

import com.hoteluq.infrastructure.memory.InMemoryCustomerCatalog;
import com.hoteluq.infrastructure.memory.InMemoryCustomerCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerAccountServiceTest {
    private InMemoryCustomerCatalog customers;
    private InMemoryCustomerCredentialRepository credentials;
    private CustomerAccountService accounts;

    @BeforeEach
    void setUp() {
        customers = new InMemoryCustomerCatalog(null);
        credentials = new InMemoryCustomerCredentialRepository();
        accounts = new CustomerAccountService(customers, credentials);
    }

    @Test
    void registersCustomerAndStoresOnlyPasswordHash() {
        var customer = accounts.register("Isabela Quintero", "isabela@example.com", "secret123");

        assertEquals(customer, customers.findByEmail("ISABELA@example.com").orElseThrow());
        var credential = credentials.findByCustomerId(customer.getId()).orElseThrow();
        assertNotEquals("secret123", credential.passwordHash());
        assertEquals(1, customers.findAll().size());
    }

    @Test
    void authenticatesCustomerWithCorrectCredentials() {
        var registered = accounts.register("Isabela Quintero", "isabela@example.com", "secret123");

        assertEquals(registered, accounts.authenticate("ISABELA@example.com", "secret123"));
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        accounts.register("First Customer", "same@example.com", "secret123");

        assertThrows(IllegalArgumentException.class,
                () -> accounts.register("Second Customer", "SAME@example.com", "other123"));
        assertEquals(1, customers.findAll().size());
    }

    @Test
    void rejectsIncorrectCredentials() {
        accounts.register("Isabela Quintero", "isabela@example.com", "secret123");

        assertThrows(InvalidCredentialsException.class,
                () -> accounts.authenticate("isabela@example.com", "wrong-password"));
    }

    @Test
    void rejectsAuthenticationForUnknownCustomer() {
        assertThrows(InvalidCredentialsException.class,
                () -> accounts.authenticate("missing@example.com", "secret123"));
    }

    @Test
    void hashesEqualPasswordsWithDifferentSalts() {
        var first = accounts.register("First Customer", "first@example.com", "secret123");
        var second = accounts.register("Second Customer", "second@example.com", "secret123");

        var firstCredential = credentials.findByCustomerId(first.getId()).orElseThrow();
        var secondCredential = credentials.findByCustomerId(second.getId()).orElseThrow();
        assertNotEquals(firstCredential.salt(), secondCredential.salt());
        assertNotEquals(firstCredential.passwordHash(), secondCredential.passwordHash());
    }

    @Test
    void rejectsMissingRequiredRegistrationData() {
        assertThrows(IllegalArgumentException.class,
                () -> accounts.register(" ", "valid@example.com", "secret123"));
        assertThrows(IllegalArgumentException.class,
                () -> accounts.register("Customer", "invalid-email", "secret123"));
        assertThrows(IllegalArgumentException.class,
                () -> accounts.register("Customer", "valid@example.com", " "));
        assertEquals(0, customers.findAll().size());
    }
}
