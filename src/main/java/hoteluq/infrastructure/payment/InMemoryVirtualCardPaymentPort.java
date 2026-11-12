package com.hoteluq.infrastructure.payment;

import com.hoteluq.application.payment.InactiveCardException;
import com.hoteluq.application.payment.InsufficientBalanceException;
import com.hoteluq.application.payment.PaymentDeclinedException;
import com.hoteluq.application.payment.RefundAlreadyProcessedException;
import com.hoteluq.application.port.VirtualCardPaymentPort;
import com.hoteluq.domain.model.PaymentReceipt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Demo payment adapter. The production card module supplies this port in the full system. */
public final class InMemoryVirtualCardPaymentPort implements VirtualCardPaymentPort {
    private final Map<String, DemoCardAccount> cards = new HashMap<>();
    private final Map<String, PaymentReceipt> charges = new HashMap<>();
    private final Set<String> refundedCharges = new HashSet<>();
    private final List<PaymentMovement> movements = new ArrayList<>();

    /** Seed-only setup; card administration and recharge are intentionally outside this adapter. */
    public synchronized void registerDemoCard(String cardId, String customerId,
                                              BigDecimal openingBalance, boolean active) {
        requireText(cardId, "Card id");
        requireText(customerId, "Customer id");
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        }
        cards.put(cardId.trim(), new DemoCardAccount(customerId.trim(), openingBalance, active));
    }

    @Override
    public synchronized PaymentReceipt charge(String cardId, String customerId, BigDecimal amount) {
        DemoCardAccount account = findCard(cardId);
        validatePositiveAmount(amount);
        if (customerId == null || !account.customerId.equals(customerId.trim())) {
            throw new PaymentDeclinedException("Virtual card does not belong to this customer.");
        }
        if (!account.active) throw new InactiveCardException("Virtual card is inactive.");
        if (account.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Virtual card has insufficient balance.");
        }

        account.balance = account.balance.subtract(amount);
        LocalDateTime paidAt = LocalDateTime.now();
        String chargeId = UUID.randomUUID().toString();
        PaymentReceipt receipt = new PaymentReceipt(chargeId, cardId.trim(), amount, paidAt);
        charges.put(chargeId, receipt);
        movements.add(new PaymentMovement(chargeId, cardId.trim(), PaymentMovementType.CHARGE,
                amount, null, paidAt));
        return receipt;
    }

    @Override
    public synchronized void refund(PaymentReceipt originalPayment, BigDecimal refundAmount) {
        if (originalPayment == null) {
            throw new PaymentDeclinedException("Original payment is required for a refund.");
        }
        String chargeId = originalPayment.getTransactionId();
        PaymentReceipt recordedCharge = charges.get(chargeId);
        if (recordedCharge == null || !recordedCharge.getCardId().equals(originalPayment.getCardId())
                || recordedCharge.getAmount().compareTo(originalPayment.getAmount()) != 0) {
            throw new PaymentDeclinedException("Original card charge was not found.");
        }
        if (refundedCharges.contains(chargeId)) {
            throw new RefundAlreadyProcessedException("This payment has already been refunded.");
        }
        validatePositiveAmount(refundAmount);
        if (refundAmount.compareTo(recordedCharge.getAmount()) > 0) {
            throw new PaymentDeclinedException("Refund cannot exceed the original payment.");
        }

        DemoCardAccount account = findCard(recordedCharge.getCardId());
        account.balance = account.balance.add(refundAmount);
        String refundId = UUID.randomUUID().toString();
        movements.add(new PaymentMovement(refundId, recordedCharge.getCardId(),
                PaymentMovementType.REFUND, refundAmount, chargeId, LocalDateTime.now()));
        refundedCharges.add(chargeId);
    }

    public synchronized BigDecimal getDemoBalance(String cardId) {
        return findCard(cardId).balance;
    }

    public synchronized List<PaymentMovement> getDemoMovements() {
        return List.copyOf(movements);
    }

    private DemoCardAccount findCard(String cardId) {
        if (cardId == null || cardId.isBlank()) {
            throw new PaymentDeclinedException("Card id is required.");
        }
        DemoCardAccount account = cards.get(cardId.trim());
        if (account == null) throw new PaymentDeclinedException("Virtual card was not found.");
        return account;
    }

    private static void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentDeclinedException("Amount must be greater than zero.");
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }

    private static final class DemoCardAccount {
        private final String customerId;
        private BigDecimal balance;
        private final boolean active;

        private DemoCardAccount(String customerId, BigDecimal balance, boolean active) {
            this.customerId = customerId;
            this.balance = balance;
            this.active = active;
        }
    }
}
