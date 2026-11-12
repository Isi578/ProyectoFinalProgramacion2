package com.hoteluq.application.payment;

import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.PaymentMovementRepository;
import com.hoteluq.application.port.VirtualCardRepository;
import com.hoteluq.domain.model.PaymentReceipt;
import com.hoteluq.domain.model.VirtualCard;
import com.hoteluq.infrastructure.payment.PaymentMovement;
import com.hoteluq.infrastructure.payment.PaymentMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class PaymentService {
    private final PaymentGateway paymentGateway;
    private final VirtualCardRepository cardRepository;
    private final PaymentMovementRepository movementRepository;

    public PaymentService(PaymentGateway paymentGateway,
                          VirtualCardRepository cardRepository,
                          PaymentMovementRepository movementRepository) {
        this.paymentGateway = paymentGateway;
        this.cardRepository = cardRepository;
        this.movementRepository = movementRepository;
    }

    public synchronized PaymentReceipt charge(
            String idempotencyKey, String cardId, String customerId, BigDecimal amount) {
        requireText(idempotencyKey, "Idempotency key");
        requireText(customerId, "Customer id");
        validatePositiveAmount(amount);

        VirtualCard card = findCard(cardId);
        if (!card.getCustomerId().equals(customerId.trim())) {
            throw new PaymentDeclinedException("Virtual card does not belong to this customer.");
        }

        PaymentMovement priorCharge = movementRepository.findAll().stream()
                .filter(movement -> idempotencyKey.trim().equals(movement.getIdempotencyKey()))
                .findFirst()
                .orElse(null);
        if (priorCharge != null) {
            if (priorCharge.getType() != PaymentMovementType.CHARGE
                    || !priorCharge.getCardId().equals(card.getCardId())
                    || priorCharge.getAmount().compareTo(amount) != 0) {
                throw new PaymentDeclinedException(
                        "Idempotency key was already used for a different transaction.");
            }
            return new PaymentReceipt(priorCharge.getId(), priorCharge.getCardId(),
                    priorCharge.getAmount(), priorCharge.getOccurredAt());
        }

        if (!card.isActive()) {
            throw new InactiveCardException("Virtual card is inactive.");
        }
        validateCardBalance(card);
        if (card.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Virtual card has insufficient balance.");
        }

        PaymentReceipt receipt = paymentGateway.charge(
                idempotencyKey.trim(), card.getCardId(), customerId.trim(), amount);
        card.setBalance(card.getBalance().subtract(amount));
        cardRepository.save(card);
        movementRepository.save(new PaymentMovement(
                receipt.getTransactionId(), card.getCardId(), PaymentMovementType.CHARGE,
                amount, null, receipt.getPaidAt(), idempotencyKey.trim()));
        return receipt;
    }

    public BigDecimal getBalance(String cardId) {
        return findCard(cardId).getBalance();
    }

    public synchronized void recharge(String cardId, BigDecimal amount) {
        validatePositiveAmount(amount);
        VirtualCard card = findCard(cardId);
        if (!card.isActive()) {
            throw new InactiveCardException("Virtual card is inactive.");
        }
        validateCardBalance(card);

        paymentGateway.recharge(card.getCardId(), amount);
        card.setBalance(card.getBalance().add(amount));
        cardRepository.save(card);
        movementRepository.save(new PaymentMovement(
                UUID.randomUUID().toString(), card.getCardId(), PaymentMovementType.RECHARGE,
                amount, null, LocalDateTime.now()));
    }

    public List<PaymentMovement> getMovements(String cardId) {
        return movementRepository.findByCardId(cardId);
    }

    public synchronized void refund(PaymentReceipt originalPayment, BigDecimal refundAmount) {
        if (originalPayment == null) {
            throw new PaymentDeclinedException("Original payment is required for a refund.");
        }
        requireText(originalPayment.getTransactionId(), "Transaction id");
        requireText(originalPayment.getCardId(), "Card id");
        validatePositiveAmount(originalPayment.getAmount());
        validatePositiveAmount(refundAmount);
        if (refundAmount.compareTo(originalPayment.getAmount()) > 0) {
            throw new PaymentDeclinedException("Refund cannot exceed the original payment.");
        }

        VirtualCard card = findCard(originalPayment.getCardId());
        validateCardBalance(card);
        boolean alreadyRefunded = movementRepository.findByCardId(card.getCardId()).stream()
                .anyMatch(movement -> movement.getType() == PaymentMovementType.REFUND
                        && originalPayment.getTransactionId().equals(movement.getRelatedChargeId()));
        if (alreadyRefunded) {
            throw new RefundAlreadyProcessedException("This payment has already been refunded.");
        }

        paymentGateway.refund(originalPayment, refundAmount);
        card.setBalance(card.getBalance().add(refundAmount));
        cardRepository.save(card);
        movementRepository.save(new PaymentMovement(
                UUID.randomUUID().toString(), card.getCardId(), PaymentMovementType.REFUND,
                refundAmount, originalPayment.getTransactionId(), LocalDateTime.now()));
    }

    private VirtualCard findCard(String cardId) {
        requireText(cardId, "Card id");
        return cardRepository.findById(cardId.trim())
                .orElseThrow(() -> new PaymentDeclinedException("Virtual card was not found."));
    }

    private static void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentDeclinedException("Amount must be greater than zero.");
        }
    }

    private static void validateCardBalance(VirtualCard card) {
        if (card.getBalance() == null || card.getBalance().signum() < 0) {
            throw new PaymentDeclinedException("Virtual card has an invalid balance.");
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new PaymentDeclinedException(label + " is required.");
        }
    }
}
