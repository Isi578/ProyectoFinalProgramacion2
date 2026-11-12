package com.hoteluq.application.payment;

import com.hoteluq.domain.model.PaymentReceipt;
import com.hoteluq.domain.model.VirtualCard;
import com.hoteluq.infrastructure.memory.InMemoryPaymentMovementRepository;
import com.hoteluq.infrastructure.memory.InMemoryVirtualCardRepository;
import com.hoteluq.infrastructure.payment.InMemoryVirtualCardPaymentPort;
import com.hoteluq.infrastructure.payment.PaymentMovement;
import com.hoteluq.infrastructure.payment.PaymentMovementType;
import com.hoteluq.infrastructure.payment.VirtualCardPaymentAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentServiceTest {
    private static final String CARD_ID = "CARD-TEST-001";
    private static final String CUSTOMER_ID = "CUSTOMER-TEST-001";
    private static final String ADMIN_ID = "ADMIN-TEST-001";

    private InMemoryVirtualCardPaymentPort cardPayments;
    private InMemoryVirtualCardRepository cards;
    private InMemoryPaymentMovementRepository movements;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        cardPayments = new InMemoryVirtualCardPaymentPort();
        cards = new InMemoryVirtualCardRepository();
        movements = new InMemoryPaymentMovementRepository();
        paymentService = new PaymentService(
                new VirtualCardPaymentAdapter(cardPayments),
                cards,
                movements,
                Set.of(ADMIN_ID));
        addCard(CARD_ID, new BigDecimal("1000.00"), true);
    }

    @Test
    void chargesSuccessfullyAndRecordsMovement() {
        PaymentReceipt receipt = paymentService.charge(
                "charge-success", CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));

        assertNotNull(receipt);
        assertEquals(CARD_ID, receipt.getCardId());
        assertEquals(0, new BigDecimal("875.00").compareTo(paymentService.getBalance(CARD_ID)));
        assertEquals(1, movements.findByCardId(CARD_ID).size());
        assertEquals(PaymentMovementType.CHARGE,
                movements.findByCardId(CARD_ID).get(0).getType());
        assertEquals(0, new BigDecimal("875.00").compareTo(cardPayments.getDemoBalance(CARD_ID)));
    }

    @Test
    void rejectsChargeWhenBalanceIsInsufficient() {
        assertThrows(InsufficientBalanceException.class,
                () -> paymentService.charge(
                        "charge-too-large", CARD_ID, CUSTOMER_ID, new BigDecimal("1000.01")));
        assertEquals(0, new BigDecimal("1000.00").compareTo(paymentService.getBalance(CARD_ID)));
        assertEquals(0, movements.findAll().size());
    }

    @Test
    void rejectsChargeWhenCardIsInactive() {
        addCard("CARD-INACTIVE", new BigDecimal("1000.00"), false);

        assertThrows(InactiveCardException.class,
                () -> paymentService.charge(
                        "charge-inactive", "CARD-INACTIVE", CUSTOMER_ID, new BigDecimal("20.00")));
        assertEquals(0, movements.findAll().size());
    }

    @Test
    void rejectsChargeWhenCardDoesNotExist() {
        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.charge(
                        "charge-missing", "CARD-MISSING", CUSTOMER_ID, new BigDecimal("20.00")));
        assertEquals(0, movements.findAll().size());
    }

    @Test
    void rejectsNegativeChargeAmount() {
        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.charge(
                        "charge-negative", CARD_ID, CUSTOMER_ID, new BigDecimal("-1.00")));
    }

    @Test
    void rejectsZeroChargeAmount() {
        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.charge(
                        "charge-zero", CARD_ID, CUSTOMER_ID, BigDecimal.ZERO));
    }

    @Test
    void rechargesSuccessfullyAndRecordsRechargeMovement() {
        paymentService.recharge(ADMIN_ID, CARD_ID, new BigDecimal("250.00"));

        assertEquals(0, new BigDecimal("1250.00").compareTo(paymentService.getBalance(CARD_ID)));
        assertEquals(0, new BigDecimal("1250.00").compareTo(cardPayments.getDemoBalance(CARD_ID)));
        List<PaymentMovement> cardMovements = movements.findByCardId(CARD_ID);
        assertEquals(1, cardMovements.size());
        assertEquals(PaymentMovementType.RECHARGE, cardMovements.get(0).getType());
        assertEquals(0, new BigDecimal("250.00").compareTo(cardMovements.get(0).getAmount()));
    }

    @Test
    void rejectsRechargeFromUnauthorizedUser() {
        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.recharge("CUSTOMER-UNAUTHORIZED", CARD_ID,
                        new BigDecimal("250.00")));
        assertEquals(0, new BigDecimal("1000.00").compareTo(paymentService.getBalance(CARD_ID)));
        assertEquals(0, movements.findAll().size());
    }

    @Test
    void refundsSuccessfullyAndRecordsRefundMovement() {
        PaymentReceipt receipt = paymentService.charge(
                "charge-to-refund", CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));

        paymentService.refund(receipt, new BigDecimal("80.00"));

        assertEquals(0, new BigDecimal("955.00").compareTo(paymentService.getBalance(CARD_ID)));
        List<PaymentMovement> cardMovements = movements.findByCardId(CARD_ID);
        assertEquals(2, cardMovements.size());
        assertEquals(1, cardMovements.stream()
                .filter(movement -> movement.getType() == PaymentMovementType.CHARGE).count());
        PaymentMovement refund = cardMovements.stream()
                .filter(movement -> movement.getType() == PaymentMovementType.REFUND)
                .findFirst()
                .orElseThrow();
        assertEquals(0, new BigDecimal("80.00").compareTo(refund.getAmount()));
        assertEquals(receipt.getTransactionId(), refund.getRelatedChargeId());
    }

    @Test
    void rejectsDuplicateRefund() {
        PaymentReceipt receipt = paymentService.charge(
                "charge-refund-once", CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));
        paymentService.refund(receipt, new BigDecimal("125.00"));

        assertThrows(RefundAlreadyProcessedException.class,
                () -> paymentService.refund(receipt, new BigDecimal("125.00")));
        assertEquals(2, movements.findAll().size());
    }

    private void addCard(String cardId, BigDecimal balance, boolean active) {
        cards.save(new VirtualCard(
                cardId, CUSTOMER_ID, balance, active, LocalDateTime.now()));
        cardPayments.registerDemoCard(cardId, CUSTOMER_ID, balance, active);
    }
}
