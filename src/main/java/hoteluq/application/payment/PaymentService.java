package com.hoteluq.application.payment;

import com.hoteluq.application.contract.CardRechargeUseCase;
import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.PaymentMovementRepository;
import com.hoteluq.application.port.VirtualCardRepository;
import com.hoteluq.domain.model.PaymentReceipt;
import com.hoteluq.domain.model.PaymentMovement;
import com.hoteluq.domain.model.PaymentMovementType;
import com.hoteluq.domain.model.VirtualCard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PaymentService implements CardRechargeUseCase, PaymentGateway {
    private final PaymentGateway paymentGateway;
    private final VirtualCardRepository cardRepository;
    private final PaymentMovementRepository movementRepository;
    private final Set<String> authorizedAdministratorIds;

    public PaymentService(PaymentGateway paymentGateway,
                          VirtualCardRepository cardRepository,
                          PaymentMovementRepository movementRepository) {
        this(paymentGateway, cardRepository, movementRepository, Set.of());
    }

    public PaymentService(PaymentGateway paymentGateway,
                          VirtualCardRepository cardRepository,
                          PaymentMovementRepository movementRepository,
                          Set<String> authorizedAdministratorIds) {
        this.paymentGateway = paymentGateway;
        this.cardRepository = cardRepository;
        this.movementRepository = movementRepository;
        if (authorizedAdministratorIds == null) {
            throw new IllegalArgumentException("Authorized administrators are required.");
        }
        this.authorizedAdministratorIds = new HashSet<>();
        for (String administratorId : authorizedAdministratorIds) {
            if (administratorId == null || administratorId.isBlank()) {
                throw new IllegalArgumentException("Administrator id is required.");
            }
            this.authorizedAdministratorIds.add(administratorId.trim());
        }
    }

    @Override
    public PaymentReceipt charge(String cardId, String customerId, BigDecimal amount) {
        return charge(UUID.randomUUID().toString(), cardId, customerId, amount);
    }

    @Override
    public synchronized PaymentReceipt charge(
            String idempotencyKey, String cardId, String customerId, BigDecimal amount) {
        requireText(idempotencyKey, "Idempotency key");
        requireText(customerId, "Customer id");
        validatePositiveAmount(amount);

        VirtualCard card = findCard(cardId);
        if (!card.getCustomerId().equals(customerId.trim())) {
            throw new PaymentDeclinedException("Virtual card does not belong to this customer.");
        }

        PaymentMovement priorCharge = movementRepository
                .findByIdempotencyKey(idempotencyKey.trim())
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

    @Override
    public synchronized void recharge(String administratorId, String cardId, BigDecimal amount) {
        requireAuthorizedAdministrator(administratorId);
        validatePositiveAmount(amount);
        VirtualCard card = findCard(cardId);
        if (!card.isActive()) {
            throw new InactiveCardException("Virtual card is inactive.");
        }
        validateCardBalance(card);

        paymentGateway.recharge(administratorId.trim(), card.getCardId(), amount);
        card.setBalance(card.getBalance().add(amount));
        cardRepository.save(card);
        movementRepository.save(new PaymentMovement(
                UUID.randomUUID().toString(), card.getCardId(), PaymentMovementType.RECHARGE,
                amount, null, LocalDateTime.now()));
    }

    public List<PaymentMovement> getMovements(String cardId) {
        return movementRepository.findByCardId(cardId);
    }

    @Override
    public synchronized void refund(PaymentReceipt originalPayment, BigDecimal refundAmount) {
        requireOriginalPayment(originalPayment);
        validatePositiveAmount(refundAmount);
        String idempotencyKey = "legacy-refund:" + originalPayment.getTransactionId()
                + ":" + refundAmount.stripTrailingZeros().toPlainString();
        refund(idempotencyKey, originalPayment, refundAmount);
    }

    @Override
    public synchronized void refund(
            String idempotencyKey, PaymentReceipt originalPayment, BigDecimal refundAmount) {
        requireText(idempotencyKey, "Idempotency key");
        requireOriginalPayment(originalPayment);
        validatePositiveAmount(refundAmount);
        if (refundAmount.compareTo(originalPayment.getAmount()) > 0) {
            throw new PaymentDeclinedException("Refund cannot exceed the original payment.");
        }

        PaymentMovement priorRefund = movementRepository
                .findByIdempotencyKey(idempotencyKey.trim())
                .orElse(null);
        if (priorRefund != null) {
            if (priorRefund.getType() != PaymentMovementType.REFUND
                    || !priorRefund.getCardId().equals(originalPayment.getCardId())
                    || !originalPayment.getTransactionId().equals(priorRefund.getRelatedChargeId())
                    || priorRefund.getAmount().compareTo(refundAmount) != 0) {
                throw new PaymentDeclinedException(
                        "Idempotency key was already used for a different transaction.");
            }
            return;
        }

        VirtualCard card = findCard(originalPayment.getCardId());
        validateCardBalance(card);
        BigDecimal refundedSoFar = movementRepository
                .findByRelatedChargeId(originalPayment.getTransactionId()).stream()
                .filter(movement -> movement.getType() == PaymentMovementType.REFUND)
                .map(PaymentMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingAmount = originalPayment.getAmount().subtract(refundedSoFar);
        if (refundAmount.compareTo(remainingAmount) > 0) {
            throw new PaymentDeclinedException("Refund exceeds the remaining refundable amount.");
        }

        paymentGateway.refund(idempotencyKey.trim(), originalPayment, refundAmount);
        card.setBalance(card.getBalance().add(refundAmount));
        cardRepository.save(card);
        movementRepository.save(new PaymentMovement(
                UUID.randomUUID().toString(), card.getCardId(), PaymentMovementType.REFUND,
                refundAmount, originalPayment.getTransactionId(), LocalDateTime.now(),
                idempotencyKey.trim()));
    }

    private static void requireOriginalPayment(PaymentReceipt originalPayment) {
        if (originalPayment == null) {
            throw new PaymentDeclinedException("Original payment is required for a refund.");
        }
        requireText(originalPayment.getTransactionId(), "Transaction id");
        requireText(originalPayment.getCardId(), "Card id");
        validatePositiveAmount(originalPayment.getAmount());
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

    private void requireAuthorizedAdministrator(String administratorId) {
        requireText(administratorId, "Administrator id");
        if (!authorizedAdministratorIds.contains(administratorId.trim())) {
            throw new PaymentDeclinedException("Administrator is not authorized to recharge cards.");
        }
    }
}
