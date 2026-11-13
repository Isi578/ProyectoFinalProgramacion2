package com.hoteluq.infrastructure.factory;

import com.hoteluq.application.payment.PaymentDeclinedException;
import com.hoteluq.application.customer.InvalidCredentialsException;
import com.hoteluq.domain.model.PaymentMovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryHotelComponentsFactoryTest {
    private static final String CARD_ID = "CARD-ANA-001";

    @Test
    void allowsConfiguredAdministratorToRechargeAndRecordsMovement() {
        HotelComponents components = new InMemoryHotelComponentsFactory().createComponents();

        components.getCardRechargeUseCase().recharge(
                InMemoryHotelComponentsFactory.DEMO_ADMINISTRATOR_ID,
                CARD_ID,
                new BigDecimal("250.00"));

        var recharge = components.getPaymentMovementRepository().findByCardId(CARD_ID).get(0);
        assertEquals(PaymentMovementType.RECHARGE, recharge.getType());
        assertEquals(0, new BigDecimal("250.00").compareTo(recharge.getAmount()));
    }

    @Test
    void rejectsUnconfiguredUserAndDoesNotRecordMovement() {
        HotelComponents components = new InMemoryHotelComponentsFactory().createComponents();

        assertThrows(PaymentDeclinedException.class,
                () -> components.getCardRechargeUseCase().recharge(
                        "GUEST-ANA-001", CARD_ID, new BigDecimal("250.00")));
        assertEquals(0, components.getPaymentMovementRepository().findAll().size());
    }

    @Test
    void demoCustomerCanAuthenticateAndRejectsIncorrectPassword() {
        HotelComponents components = new InMemoryHotelComponentsFactory().createComponents();

        var customer = components.getCustomerAccountUseCase().authenticate(
                "ANA@example.com", InMemoryHotelComponentsFactory.DEMO_CUSTOMER_PASSWORD);

        assertEquals("GUEST-ANA-001", customer.getId());
        assertThrows(InvalidCredentialsException.class,
                () -> components.getCustomerAccountUseCase().authenticate(
                        "ana@example.com", "incorrect-demo-password"));
    }
}
