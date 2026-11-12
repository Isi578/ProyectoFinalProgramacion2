package com.hoteluq.application.payment;

import com.hoteluq.domain.model.PaymentReceipt;
import com.hoteluq.domain.model.PaymentMovement;
import com.hoteluq.domain.model.PaymentMovementType;
import com.hoteluq.domain.model.VirtualCard;
import com.hoteluq.application.port.PaymentMovementRepository;
import com.hoteluq.infrastructure.memory.InMemoryPaymentMovementRepository;
import com.hoteluq.infrastructure.memory.InMemoryVirtualCardRepository;
import com.hoteluq.infrastructure.payment.InMemoryVirtualCardPaymentPort;
import com.hoteluq.infrastructure.payment.VirtualCardPaymentAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        assertEquals(0, new BigDecimal("875.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
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
    void repeatedChargeWithSameIdempotencyKeyDoesNotDebitTwice() {
        PaymentReceipt first = paymentService.charge(
                "reservation-operation-001", CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));
        PaymentReceipt retry = paymentService.charge(
                "reservation-operation-001", CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));

        assertEquals(first.getTransactionId(), retry.getTransactionId());
        assertEquals(0, new BigDecimal("875.00").compareTo(paymentService.getBalance(CARD_ID)));
        assertEquals(1, movements.findByCardId(CARD_ID).size());
    }

    @Test
    void rejectsReuseOfIdempotencyKeyWithDifferentChargeData() {
        paymentService.charge(
                "reservation-operation-002", CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));

        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.charge(
                        "reservation-operation-002", CARD_ID, CUSTOMER_ID, new BigDecimal("126.00")));
        assertEquals(0, new BigDecimal("875.00").compareTo(paymentService.getBalance(CARD_ID)));
        assertEquals(1, movements.findByCardId(CARD_ID).size());
    }

    @Test
    void retryingSameReservationOperationReusesItsReceipt() {
        PaymentReceipt first = paymentService.charge(
                "reservation-key-retry", "reservation-reference-001",
                CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));
        PaymentReceipt retry = paymentService.charge(
                "reservation-key-retry", "reservation-reference-001",
                CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));

        assertEquals(first.getTransactionId(), retry.getTransactionId());
        assertEquals(0, new BigDecimal("875.00").compareTo(paymentService.getBalance(CARD_ID)));
        assertEquals(1, movements.findByCardId(CARD_ID).size());
    }

    @Test
    void differentReservationOperationsChargeIndependentlyForSameCardAndAmount() {
        PaymentReceipt firstReservation = paymentService.charge(
                "reservation-key-101", "reservation-reference-101",
                CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));
        PaymentReceipt secondReservation = paymentService.charge(
                "reservation-key-102", "reservation-reference-102",
                CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));

        assertNotEquals(firstReservation.getTransactionId(), secondReservation.getTransactionId());
        assertEquals(0, new BigDecimal("750.00").compareTo(paymentService.getBalance(CARD_ID)));
        assertEquals(2, movements.findByCardId(CARD_ID).size());
    }

    @Test
    void rejectsReuseOfReservationKeyForDifferentReservationReference() {
        paymentService.charge(
                "reservation-key-reused", "reservation-reference-original",
                CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));

        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.charge(
                        "reservation-key-reused", "reservation-reference-different",
                        CARD_ID, CUSTOMER_ID, new BigDecimal("125.00")));
        assertEquals(0, new BigDecimal("875.00").compareTo(paymentService.getBalance(CARD_ID)));
        assertEquals(1, movements.findByCardId(CARD_ID).size());
    }

    @Test
    void rechargesSuccessfullyAndRecordsRechargeMovement() {
        paymentService.recharge(ADMIN_ID, CARD_ID, new BigDecimal("250.00"));

        assertEquals(0, new BigDecimal("1250.00").compareTo(paymentService.getBalance(CARD_ID)));
        assertEquals(0, new BigDecimal("1250.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
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
    void refundsCompletelyAndRecordsRefundMovement() {
        PaymentReceipt receipt = paymentService.charge(
                "charge-to-refund", CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));

        paymentService.refund("refund-complete", receipt, new BigDecimal("125.00"));

        assertEquals(0, new BigDecimal("1000.00").compareTo(paymentService.getBalance(CARD_ID)));
        List<PaymentMovement> cardMovements = movements.findByCardId(CARD_ID);
        assertEquals(2, cardMovements.size());
        assertEquals(1, cardMovements.stream()
                .filter(movement -> movement.getType() == PaymentMovementType.CHARGE).count());
        PaymentMovement refund = cardMovements.stream()
                .filter(movement -> movement.getType() == PaymentMovementType.REFUND)
                .findFirst()
                .orElseThrow();
        assertEquals(0, new BigDecimal("125.00").compareTo(refund.getAmount()));
        assertEquals(receipt.getTransactionId(), refund.getRelatedChargeId());
    }

    @Test
    void allowsTwoPartialRefundsWithoutExceedingOriginalAmount() {
        PaymentReceipt receipt = paymentService.charge(
                "charge-two-refunds", CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));

        paymentService.refund("refund-part-1", receipt, new BigDecimal("50.00"));
        paymentService.refund("refund-part-2", receipt, new BigDecimal("75.00"));

        assertEquals(0, new BigDecimal("1000.00").compareTo(paymentService.getBalance(CARD_ID)));
        List<PaymentMovement> refunds = movements.findByRelatedChargeId(receipt.getTransactionId());
        assertEquals(2, refunds.size());
        assertEquals(0, new BigDecimal("125.00").compareTo(refunds.stream()
                .map(PaymentMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    @Test
    void rejectsRefundThatExceedsRemainingAmount() {
        PaymentReceipt receipt = paymentService.charge(
                "charge-refund-limit", CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));
        paymentService.refund("refund-first-part", receipt, new BigDecimal("80.00"));

        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.refund("refund-too-much", receipt, new BigDecimal("45.01")));
        assertEquals(2, movements.findAll().size());
    }

    @Test
    void repeatedRefundWithSameIdempotencyKeyDoesNotCreditTwice() {
        PaymentReceipt receipt = paymentService.charge(
                "charge-refund-retry", CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));

        paymentService.refund("refund-retry", receipt, new BigDecimal("40.00"));
        paymentService.refund("refund-retry", receipt, new BigDecimal("40.00"));

        assertEquals(0, new BigDecimal("915.00").compareTo(paymentService.getBalance(CARD_ID)));
        assertEquals(2, movements.findAll().size());
    }

    @Test
    void rejectsInvalidRefundAmounts() {
        PaymentReceipt receipt = paymentService.charge(
                "charge-refund-invalid", CARD_ID, CUSTOMER_ID, new BigDecimal("125.00"));

        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.refund("refund-zero", receipt, BigDecimal.ZERO));
        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.refund("refund-negative", receipt, new BigDecimal("-1.00")));
        assertEquals(1, movements.findAll().size());
    }

    @Test
    void doesNotChangeBalanceWhenMovementPersistenceFailsAfterWriting() {
        InMemoryPaymentMovementRepository persistedMovements =
                new InMemoryPaymentMovementRepository();
        PaymentMovementRepository failingMovements = new PaymentMovementRepository() {
            private boolean failOnSave = true;

            @Override
            public PaymentMovement save(PaymentMovement movement) {
                PaymentMovement saved = persistedMovements.save(movement);
                if (failOnSave) {
                    failOnSave = false;
                    throw new IllegalStateException("Simulated failure after movement write.");
                }
                return saved;
            }

            @Override
            public void deleteById(String movementId) {
                persistedMovements.deleteById(movementId);
            }

            @Override
            public List<PaymentMovement> findByCardId(String cardId) {
                return persistedMovements.findByCardId(cardId);
            }

            @Override
            public List<PaymentMovement> findByRelatedChargeId(String relatedChargeId) {
                return persistedMovements.findByRelatedChargeId(relatedChargeId);
            }

            @Override
            public java.util.Optional<PaymentMovement> findByIdempotencyKey(String key) {
                return persistedMovements.findByIdempotencyKey(key);
            }

            @Override
            public List<PaymentMovement> findAll() {
                return persistedMovements.findAll();
            }
        };
        PaymentService service = new PaymentService(
                new VirtualCardPaymentAdapter(cardPayments), cards, failingMovements);

        assertThrows(PaymentPersistenceException.class,
                () -> service.charge(
                        "charge-persistence-failure", CARD_ID, CUSTOMER_ID,
                        new BigDecimal("125.00")));

        assertEquals(0, new BigDecimal("1000.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
        assertEquals(0, persistedMovements.findAll().size());
    }

    private void addCard(String cardId, BigDecimal balance, boolean active) {
        cards.save(new VirtualCard(
                cardId, CUSTOMER_ID, balance, active, LocalDateTime.now()));
    }
}
