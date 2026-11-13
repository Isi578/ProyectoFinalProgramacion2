package com.hoteluq.application.payment;

import com.hoteluq.domain.model.PaymentReceipt;
import com.hoteluq.domain.model.PaymentMovement;
import com.hoteluq.domain.model.PaymentMovementType;
import com.hoteluq.domain.model.PaymentMovementStatus;
import com.hoteluq.domain.model.VirtualCard;
import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.PaymentMovementRepository;
import com.hoteluq.application.port.VirtualCardRepository;
import com.hoteluq.infrastructure.memory.InMemoryPaymentMovementRepository;
import com.hoteluq.infrastructure.memory.InMemoryVirtualCardRepository;
import com.hoteluq.infrastructure.payment.InMemoryVirtualCardPaymentPort;
import com.hoteluq.infrastructure.payment.VirtualCardPaymentAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    void rejectsChargeWhenCardBelongsToAnotherCustomer() {
        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.charge(
                        "charge-wrong-owner", CARD_ID, "CUSTOMER-OTHER",
                        new BigDecimal("20.00")));
        assertEquals(0, movements.findAll().size());
        assertEquals(0, new BigDecimal("1000.00").compareTo(paymentService.getBalance(CARD_ID)));
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
    void rejectsBlankRechargeAdministratorId() {
        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.recharge(" ", CARD_ID, new BigDecimal("25.00")));
        assertEquals(0, movements.findAll().size());
    }

    @Test
    void rejectsRechargeForMissingCard() {
        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.recharge(ADMIN_ID, "CARD-MISSING", new BigDecimal("25.00")));
        assertEquals(0, movements.findAll().size());
    }

    @Test
    void rejectsRechargeForInactiveCard() {
        addCard("CARD-INACTIVE-RECHARGE", new BigDecimal("100.00"), false);

        assertThrows(InactiveCardException.class,
                () -> paymentService.recharge(
                        ADMIN_ID, "CARD-INACTIVE-RECHARGE", new BigDecimal("25.00")));
        assertEquals(0, movements.findAll().size());
        assertEquals(0, new BigDecimal("100.00").compareTo(
                cards.findById("CARD-INACTIVE-RECHARGE").orElseThrow().getBalance()));
    }

    @Test
    void rejectsInvalidRechargeAmounts() {
        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.recharge(ADMIN_ID, CARD_ID, BigDecimal.ZERO));
        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.recharge(ADMIN_ID, CARD_ID, new BigDecimal("-1.00")));
        assertThrows(PaymentDeclinedException.class,
                () -> paymentService.recharge(ADMIN_ID, CARD_ID, null));
        assertEquals(0, movements.findAll().size());
        assertEquals(0, new BigDecimal("1000.00").compareTo(paymentService.getBalance(CARD_ID)));
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
    void movementIntentFailureDoesNotCallGatewayOrChangeBalance() {
        InMemoryPaymentMovementRepository persistedMovements =
                new InMemoryPaymentMovementRepository();
        PaymentMovementRepository failingMovements =
                failingMovementRepository(persistedMovements, 1, false);
        CountingIdempotentGateway gateway = new CountingIdempotentGateway();
        PaymentService service = new PaymentService(gateway, cards, failingMovements);

        assertThrows(PaymentPersistenceException.class,
                () -> service.charge(
                        "charge-persistence-failure", CARD_ID, CUSTOMER_ID,
                        new BigDecimal("125.00")));

        assertEquals(0, new BigDecimal("1000.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
        assertEquals(0, persistedMovements.findAll().size());
        assertEquals(0, gateway.successfulChargeCount);
    }

    @Test
    void retryAfterGatewaySuccessAndMovementUpdateFailureDoesNotDuplicateCharge() {
        InMemoryPaymentMovementRepository persistedMovements =
                new InMemoryPaymentMovementRepository();
        PaymentMovementRepository failingMovements =
                failingMovementRepository(persistedMovements, 2, false);
        CountingIdempotentGateway gateway = new CountingIdempotentGateway();
        PaymentService service = new PaymentService(gateway, cards, failingMovements);

        assertThrows(PaymentPersistenceException.class,
                () -> service.charge(
                        "charge-movement-update-failure", CARD_ID, CUSTOMER_ID,
                        new BigDecimal("125.00")));
        assertEquals(PaymentMovementStatus.PENDING,
                persistedMovements.findByIdempotencyKey("charge-movement-update-failure")
                        .orElseThrow().getStatus());
        assertEquals(0, new BigDecimal("1000.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));

        PaymentReceipt retry = service.charge(
                "charge-movement-update-failure", CARD_ID, CUSTOMER_ID,
                new BigDecimal("125.00"));

        assertEquals(1, gateway.successfulChargeCount);
        assertEquals(2, gateway.chargeAttemptCount);
        assertEquals(0, new BigDecimal("875.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
        assertEquals(PaymentMovementStatus.COMPLETED,
                persistedMovements.findByIdempotencyKey("charge-movement-update-failure")
                        .orElseThrow().getStatus());
        assertEquals(retry.getTransactionId(),
                persistedMovements.findByIdempotencyKey("charge-movement-update-failure")
                        .orElseThrow().getId());
    }

    @Test
    void balanceSaveFailureLeavesRecoverableMovementAndRetryAppliesOnce() {
        InMemoryPaymentMovementRepository persistedMovements =
                new InMemoryPaymentMovementRepository();
        VirtualCardRepository failingCards = failingCardRepository(cards, 1, false);
        CountingIdempotentGateway gateway = new CountingIdempotentGateway();
        PaymentService service = new PaymentService(gateway, failingCards, persistedMovements);

        assertThrows(PaymentPersistenceException.class,
                () -> service.charge(
                        "charge-card-save-failure", CARD_ID, CUSTOMER_ID,
                        new BigDecimal("125.00")));
        assertEquals(0, new BigDecimal("1000.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
        assertEquals(PaymentMovementStatus.EXTERNAL_SUCCEEDED,
                persistedMovements.findByIdempotencyKey("charge-card-save-failure")
                        .orElseThrow().getStatus());

        service.charge("charge-card-save-failure", CARD_ID, CUSTOMER_ID,
                new BigDecimal("125.00"));
        assertEquals(1, gateway.successfulChargeCount);
        assertEquals(0, new BigDecimal("875.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
        assertEquals(1, persistedMovements.findAll().size());
        assertEquals(PaymentMovementStatus.COMPLETED,
                persistedMovements.findByIdempotencyKey("charge-card-save-failure")
                        .orElseThrow().getStatus());
    }

    @Test
    void refundRetryAfterCardSaveFailureDoesNotCreditTwice() {
        PaymentReceipt charge = paymentService.charge(
                "charge-before-refund-retry", CARD_ID, CUSTOMER_ID,
                new BigDecimal("125.00"));
        VirtualCardRepository failingCards = failingCardRepository(cards, 1, false);
        PaymentService service = new PaymentService(
                new VirtualCardPaymentAdapter(cardPayments), failingCards, movements);

        assertThrows(PaymentPersistenceException.class,
                () -> service.refund(
                        "refund-card-save-failure", charge, new BigDecimal("40.00")));
        assertEquals(0, new BigDecimal("875.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));

        service.refund("refund-card-save-failure", charge, new BigDecimal("40.00"));

        assertEquals(0, new BigDecimal("915.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
        assertEquals(1, movements.findByRelatedChargeId(charge.getTransactionId()).stream()
                .filter(movement -> movement.getType() == PaymentMovementType.REFUND).count());
    }

    @Test
    void refundRetryAfterAmbiguousGatewayResultDoesNotRefundExternallyTwice() {
        CountingIdempotentGateway gateway = new CountingIdempotentGateway();
        PaymentService service = new PaymentService(gateway, cards, movements);
        PaymentReceipt charge = service.charge(
                "charge-before-ambiguous-refund", CARD_ID, CUSTOMER_ID,
                new BigDecimal("125.00"));
        gateway.failAfterFirstSuccessfulRefund = true;

        assertThrows(PaymentGatewayException.class,
                () -> service.refund(
                        "ambiguous-refund", charge, new BigDecimal("40.00")));
        assertEquals(0, new BigDecimal("875.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));

        service.refund("ambiguous-refund", charge, new BigDecimal("40.00"));

        assertEquals(1, gateway.successfulRefundCount);
        assertEquals(2, gateway.refundAttemptCount);
        assertEquals(0, new BigDecimal("915.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
        assertEquals(1, movements.findByRelatedChargeId(charge.getTransactionId()).stream()
                .filter(movement -> movement.getType() == PaymentMovementType.REFUND).count());
    }

    @Test
    void refundRetryAfterJournalUpdateFailureDoesNotRepeatExternalRefund() {
        CountingIdempotentGateway gateway = new CountingIdempotentGateway();
        PaymentService chargeService = new PaymentService(gateway, cards, movements);
        PaymentReceipt charge = chargeService.charge(
                "charge-before-journal-refund-failure", CARD_ID, CUSTOMER_ID,
                new BigDecimal("125.00"));
        PaymentMovementRepository failingMovements =
                failingMovementRepository(movements, 2, false);
        PaymentService refundService = new PaymentService(gateway, cards, failingMovements);

        assertThrows(PaymentPersistenceException.class,
                () -> refundService.refund(
                        "refund-journal-update-failure", charge, new BigDecimal("40.00")));
        assertEquals(0, new BigDecimal("875.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));

        refundService.refund("refund-journal-update-failure", charge, new BigDecimal("40.00"));

        assertEquals(1, gateway.successfulRefundCount);
        assertEquals(2, gateway.refundAttemptCount);
        assertEquals(0, new BigDecimal("915.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
        assertEquals(1, movements.findByRelatedChargeId(charge.getTransactionId()).stream()
                .filter(movement -> movement.getType() == PaymentMovementType.REFUND).count());
    }

    @Test
    void journalWriteThatThrowsAfterPersistenceIsRecoveredWithoutDuplicateMovement() {
        InMemoryPaymentMovementRepository persistedMovements =
                new InMemoryPaymentMovementRepository();
        PaymentMovementRepository failingMovements =
                failingMovementRepository(persistedMovements, 2, true);
        CountingIdempotentGateway gateway = new CountingIdempotentGateway();
        PaymentService service = new PaymentService(gateway, cards, failingMovements);

        PaymentReceipt receipt = service.charge(
                "charge-journal-write-then-throw", CARD_ID, CUSTOMER_ID,
                new BigDecimal("125.00"));
        PaymentReceipt retry = service.charge(
                "charge-journal-write-then-throw", CARD_ID, CUSTOMER_ID,
                new BigDecimal("125.00"));

        assertEquals(receipt.getTransactionId(), retry.getTransactionId());
        assertEquals(1, gateway.successfulChargeCount);
        assertEquals(0, new BigDecimal("875.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
        assertEquals(1, persistedMovements.findAll().size());
        assertEquals(PaymentMovementStatus.COMPLETED,
                persistedMovements.findByIdempotencyKey(
                        "charge-journal-write-then-throw").orElseThrow().getStatus());
    }

    @Test
    void rechargeRetryAfterAmbiguousGatewayResultUpdatesLocalBalanceOnce() {
        CountingIdempotentGateway gateway = new CountingIdempotentGateway();
        PaymentService service = new PaymentService(
                gateway, cards, movements, Set.of(ADMIN_ID));
        gateway.failAfterFirstSuccessfulRecharge = true;

        assertThrows(PaymentGatewayException.class,
                () -> service.recharge(
                        "ambiguous-recharge", ADMIN_ID, CARD_ID, new BigDecimal("50.00")));
        assertEquals(0, new BigDecimal("1000.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));

        service.recharge("ambiguous-recharge", ADMIN_ID, CARD_ID, new BigDecimal("50.00"));

        assertEquals(1, gateway.successfulRechargeCount);
        assertEquals(2, gateway.rechargeAttemptCount);
        assertEquals(0, new BigDecimal("1050.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
        assertEquals(1, movements.findByCardId(CARD_ID).size());
    }

    @Test
    void cardRepositoryFailureAfterWriteIsDetectedWithoutApplyingBalanceTwice() {
        VirtualCardRepository failingCards = failingCardRepository(cards, 1, true);
        InMemoryPaymentMovementRepository persistedMovements =
                new InMemoryPaymentMovementRepository();
        PaymentService service = new PaymentService(
                new VirtualCardPaymentAdapter(cardPayments), failingCards, persistedMovements);

        service.charge("card-save-after-write", CARD_ID, CUSTOMER_ID,
                new BigDecimal("125.00"));
        service.charge("card-save-after-write", CARD_ID, CUSTOMER_ID,
                new BigDecimal("125.00"));

        assertEquals(0, new BigDecimal("875.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
        assertEquals(1, persistedMovements.findAll().size());
    }

    @Test
    void ambiguousExternalChargeFailureCanBeRetriedWithoutSecondExternalCharge() {
        CountingIdempotentGateway gateway = new CountingIdempotentGateway();
        gateway.failAfterFirstSuccessfulCharge = true;
        PaymentService service = new PaymentService(gateway, cards, movements);

        assertThrows(PaymentGatewayException.class,
                () -> service.charge(
                        "ambiguous-external-charge", CARD_ID, CUSTOMER_ID,
                        new BigDecimal("125.00")));
        PaymentReceipt retry = service.charge(
                "ambiguous-external-charge", CARD_ID, CUSTOMER_ID,
                new BigDecimal("125.00"));

        assertNotNull(retry);
        assertEquals(1, gateway.successfulChargeCount);
        assertEquals(2, gateway.chargeAttemptCount);
        assertEquals(0, new BigDecimal("875.00").compareTo(
                cards.findById(CARD_ID).orElseThrow().getBalance()));
    }

    private void addCard(String cardId, BigDecimal balance, boolean active) {
        cards.save(new VirtualCard(
                cardId, CUSTOMER_ID, balance, active, LocalDateTime.now()));
    }

    private static PaymentMovementRepository failingMovementRepository(
            PaymentMovementRepository delegate, int failOnSaveNumber, boolean failAfterWrite) {
        return new PaymentMovementRepository() {
            private int saveCount;

            @Override
            public PaymentMovement save(PaymentMovement movement) {
                saveCount++;
                if (saveCount == failOnSaveNumber && !failAfterWrite) {
                    throw new IllegalStateException("Simulated movement repository failure.");
                }
                PaymentMovement saved = delegate.save(movement);
                if (saveCount == failOnSaveNumber) {
                    throw new IllegalStateException("Simulated movement repository failure.");
                }
                return saved;
            }

            @Override
            public void deleteById(String movementId) {
                delegate.deleteById(movementId);
            }

            @Override
            public List<PaymentMovement> findByCardId(String cardId) {
                return delegate.findByCardId(cardId);
            }

            @Override
            public List<PaymentMovement> findByRelatedChargeId(String relatedChargeId) {
                return delegate.findByRelatedChargeId(relatedChargeId);
            }

            @Override
            public java.util.Optional<PaymentMovement> findByIdempotencyKey(String key) {
                return delegate.findByIdempotencyKey(key);
            }

            @Override
            public List<PaymentMovement> findAll() {
                return delegate.findAll();
            }
        };
    }

    private static VirtualCardRepository failingCardRepository(
            VirtualCardRepository delegate, int failOnSaveNumber, boolean failAfterWrite) {
        return new VirtualCardRepository() {
            private int saveCount;

            @Override
            public VirtualCard save(VirtualCard card) {
                saveCount++;
                if (saveCount == failOnSaveNumber && !failAfterWrite) {
                    throw new IllegalStateException("Simulated card repository failure.");
                }
                VirtualCard saved = delegate.save(card);
                if (saveCount == failOnSaveNumber) {
                    throw new IllegalStateException("Simulated card repository failure.");
                }
                return saved;
            }

            @Override
            public java.util.Optional<VirtualCard> findById(String cardId) {
                return delegate.findById(cardId);
            }

            @Override
            public List<VirtualCard> findByCustomerId(String customerId) {
                return delegate.findByCustomerId(customerId);
            }

            @Override
            public List<VirtualCard> findAll() {
                return delegate.findAll();
            }
        };
    }

    private static final class CountingIdempotentGateway implements PaymentGateway {
        private final Map<String, PaymentReceipt> chargeReceipts = new HashMap<>();
        private final Set<String> processedRecharges = new java.util.HashSet<>();
        private final Set<String> processedRefunds = new java.util.HashSet<>();
        private int chargeAttemptCount;
        private int successfulChargeCount;
        private int rechargeAttemptCount;
        private int successfulRechargeCount;
        private int refundAttemptCount;
        private int successfulRefundCount;
        private boolean failAfterFirstSuccessfulCharge;
        private boolean failAfterFirstSuccessfulRecharge;
        private boolean failAfterFirstSuccessfulRefund;

        @Override
        public PaymentReceipt charge(String cardId, String customerId, BigDecimal amount) {
            return charge(UUID.randomUUID().toString(), cardId, customerId, amount);
        }

        @Override
        public PaymentReceipt charge(
                String key, String cardId, String customerId, BigDecimal amount) {
            chargeAttemptCount++;
            PaymentReceipt receipt = chargeReceipts.get(key);
            if (receipt == null) {
                String transactionId = UUID.nameUUIDFromBytes(
                        key.getBytes(StandardCharsets.UTF_8)).toString();
                receipt = new PaymentReceipt(transactionId, cardId, amount, LocalDateTime.now());
                chargeReceipts.put(key, receipt);
                successfulChargeCount++;
            }
            if (failAfterFirstSuccessfulCharge) {
                failAfterFirstSuccessfulCharge = false;
                throw new IllegalStateException("Simulated ambiguous external charge result.");
            }
            return receipt;
        }

        @Override
        public PaymentReceipt charge(String key, String operationReference,
                                     String cardId, String customerId, BigDecimal amount) {
            return charge(key, cardId, customerId, amount);
        }

        @Override
        public void recharge(String administratorId, String cardId, BigDecimal amount) { }

        @Override
        public void recharge(String key, String administratorId, String cardId, BigDecimal amount) {
            rechargeAttemptCount++;
            if (processedRecharges.add(key)) successfulRechargeCount++;
            if (failAfterFirstSuccessfulRecharge) {
                failAfterFirstSuccessfulRecharge = false;
                throw new IllegalStateException("Simulated ambiguous external recharge result.");
            }
        }

        @Override
        public void refund(PaymentReceipt originalPayment, BigDecimal refundAmount) { }

        @Override
        public void refund(String key, PaymentReceipt originalPayment, BigDecimal refundAmount) {
            refundAttemptCount++;
            if (processedRefunds.add(key)) successfulRefundCount++;
            if (failAfterFirstSuccessfulRefund) {
                failAfterFirstSuccessfulRefund = false;
                throw new IllegalStateException("Simulated ambiguous external refund result.");
            }
        }
    }
}
