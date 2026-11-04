package com.hoteluq.infrastructure.payment;

import com.hoteluq.application.payment.InactiveCardException;
import com.hoteluq.application.payment.InsufficientBalanceException;
import com.hoteluq.application.payment.PaymentDeclinedException;
import com.hoteluq.application.payment.RefundAlreadyProcessedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InMemoryVirtualCardWallet implements VirtualCardWallet {
    private final Map<String, CardAccount> cards = new HashMap<>();
    private final Map<String, WalletTransaction> transactions = new HashMap<>();
    private final Map<String, Boolean> refundedCharges = new HashMap<>();

    @Override
    public synchronized void registerCard(String cardId, String customerId,
                                          BigDecimal openingBalance, boolean active) {
        requireText(cardId, "Card id");
        requireText(customerId, "Customer id");
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        }
        cards.put(cardId.trim(), new CardAccount(customerId.trim(), openingBalance, active));
    }

    @Override
    public synchronized WalletTransaction charge(String cardId, String customerId, BigDecimal amount) {
        CardAccount account = findCard(cardId);
        validatePositiveAmount(amount);
        if (customerId == null || !account.customerId.equals(customerId.trim())) {
            throw new PaymentDeclinedException("Virtual card does not belong to this customer.");
        }
        if (!account.active) throw new InactiveCardException("Virtual card is inactive.");
        if (account.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Virtual card has insufficient balance.");
        }

        account.balance = account.balance.subtract(amount);
        WalletTransaction movement = transaction(cardId, WalletTransactionType.CHARGE, amount, null);
        account.movements.add(movement);
        transactions.put(movement.getId(), movement);
        refundedCharges.put(movement.getId(), false);
        return movement;
    }

    @Override
    public synchronized WalletTransaction refund(String originalChargeId, BigDecimal amount) {
        WalletTransaction original = transactions.get(originalChargeId);
        if (original == null || original.getType() != WalletTransactionType.CHARGE) {
            throw new PaymentDeclinedException("Original card charge was not found.");
        }
        if (Boolean.TRUE.equals(refundedCharges.get(originalChargeId))) {
            throw new RefundAlreadyProcessedException("This payment has already been refunded.");
        }
        validatePositiveAmount(amount);
        if (amount.compareTo(original.getAmount()) > 0) {
            throw new PaymentDeclinedException("Refund cannot exceed the original payment.");
        }

        CardAccount account = findCard(original.getCardId());
        account.balance = account.balance.add(amount);
        WalletTransaction movement = transaction(
                original.getCardId(), WalletTransactionType.REFUND, amount, originalChargeId);
        account.movements.add(movement);
        transactions.put(movement.getId(), movement);
        refundedCharges.put(originalChargeId, true);
        return movement;
    }

    @Override
    public synchronized WalletTransaction rechargeByAdministrator(
            String cardId, BigDecimal amount, String administratorId) {
        requireText(administratorId, "Administrator id");
        validatePositiveAmount(amount);
        CardAccount account = findCard(cardId);
        account.balance = account.balance.add(amount);
        WalletTransaction movement = transaction(
                cardId, WalletTransactionType.RECHARGE, amount, administratorId.trim());
        account.movements.add(movement);
        transactions.put(movement.getId(), movement);
        return movement;
    }

    @Override
    public synchronized BigDecimal getBalance(String cardId) {
        return findCard(cardId).balance;
    }

    @Override
    public synchronized List<WalletTransaction> getMovements(String cardId) {
        return List.copyOf(findCard(cardId).movements);
    }

    private CardAccount findCard(String cardId) {
        if (cardId == null || cardId.isBlank()) {
            throw new PaymentDeclinedException("Card id is required.");
        }
        CardAccount account = cards.get(cardId.trim());
        if (account == null) throw new PaymentDeclinedException("Virtual card was not found.");
        return account;
    }

    private static WalletTransaction transaction(String cardId, WalletTransactionType type,
                                                 BigDecimal amount, String relatedId) {
        return new WalletTransaction(
                UUID.randomUUID().toString(), cardId.trim(), type, amount, relatedId,
                LocalDateTime.now());
    }

    private static void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentDeclinedException("Amount must be greater than zero.");
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
    }

    private static final class CardAccount {
        private final String customerId;
        private BigDecimal balance;
        private final boolean active;
        private final List<WalletTransaction> movements = new ArrayList<>();

        private CardAccount(String customerId, BigDecimal balance, boolean active) {
            this.customerId = customerId;
            this.balance = balance;
            this.active = active;
        }
    }
}