package com.hoteluq.infrastructure.factory;

import com.hoteluq.application.payment.PaymentDeclinedException;
import com.hoteluq.application.customer.InvalidCredentialsException;
import com.hoteluq.domain.model.PaymentMovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la fábrica sin registros precargados automáticamente.
 * Los clientes se crean únicamente por registro o precarga opcional.
 */
class InMemoryHotelComponentsFactoryTest {
    @Test
    void allowsConfiguredAdministratorToRechargeNewCardAndRecordsMovement() {
        HotelComponents components = new InMemoryHotelComponentsFactory().createComponents();
        var customer = components.getCustomerAccountUseCase().register(
                "Prueba Recarga", "recarga@example.com", "clave123");
        String cardId = components.getCustomerVirtualCardUseCase()
                .findCardsForCustomer(customer.getId()).get(0).getCardId();

        components.getCardRechargeUseCase().recharge(
                InMemoryHotelComponentsFactory.DEMO_ADMINISTRATOR_ID,
                cardId, new BigDecimal("250.00"));

        var recharge = components.getPaymentMovementRepository().findByCardId(cardId).get(0);
        assertEquals(PaymentMovementType.RECHARGE, recharge.getType());
        assertEquals(0, new BigDecimal("250.00").compareTo(recharge.getAmount()));
    }

    @Test
    void rejectsUnconfiguredUserAndDoesNotRecordMovement() {
        HotelComponents components = new InMemoryHotelComponentsFactory().createComponents();
        var customer = components.getCustomerAccountUseCase().register(
                "Prueba Control", "control@example.com", "clave123");
        String cardId = components.getCustomerVirtualCardUseCase()
                .findCardsForCustomer(customer.getId()).get(0).getCardId();

        assertThrows(PaymentDeclinedException.class,
                () -> components.getCardRechargeUseCase().recharge(
                        "administrador-falso", cardId, new BigDecimal("250.00")));
        assertEquals(0, components.getPaymentMovementRepository().findAll().size());
    }

    @Test
    void startsWithoutDemoCustomerAndAcceptsRegisteredCustomer() {
        HotelComponents components = new InMemoryHotelComponentsFactory().createComponents();
        assertTrue(components.getCustomerCatalog().findAll().isEmpty());
        assertThrows(InvalidCredentialsException.class,
                () -> components.getCustomerAccountUseCase().authenticate(
                        "ana@example.com", "clave-vieja"));

        var customer = components.getCustomerAccountUseCase().register(
                "Cliente Nuevo", "nuevo@example.com", "clave123");
        assertEquals(customer, components.getCustomerAccountUseCase()
                .authenticate("nuevo@example.com", "clave123"));
    }
}
