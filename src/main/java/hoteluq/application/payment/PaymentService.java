package com.hoteluq.application.payment;

import com.hoteluq.application.contract.CardRechargeUseCase;
import com.hoteluq.application.port.PaymentGateway;
import com.hoteluq.application.port.PaymentMovementRepository;
import com.hoteluq.application.port.VirtualCardRepository;
import com.hoteluq.domain.model.PaymentReceipt;
import com.hoteluq.domain.model.PaymentMovement;
import com.hoteluq.domain.model.PaymentMovementStatus;
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
        return charge(idempotencyKey, null, cardId, customerId, amount);
    }

    @Override
    public synchronized PaymentReceipt charge(
            String idempotencyKey, String operationReference, String cardId,
            String customerId, BigDecimal amount) {
        String key = requiredText(idempotencyKey, "Idempotency key");
        requireText(customerId, "Customer id");
        validatePositiveAmount(amount);

        VirtualCard card = findCard(cardId);
        if (!card.getCustomerId().equals(customerId.trim())) {
            throw new PaymentDeclinedException("Virtual card does not belong to this customer.");
        }

        PaymentMovement priorCharge = movementRepository
                .findByIdempotencyKey(key)
                .orElse(null);
        if (priorCharge != null) {
            validateSameOperation(priorCharge, PaymentMovementType.CHARGE, card.getCardId(),
                    amount, null, operationReference);
            return finishCharge(priorCharge, card, key, customerId.trim(), operationReference);
        }

        if (!card.isActive()) {
            throw new InactiveCardException("Virtual card is inactive.");
        }
        validateCardBalance(card);
        BigDecimal reservedCharges = movementRepository.findByCardId(card.getCardId()).stream()
                .filter(movement -> movement.getType() == PaymentMovementType.CHARGE)
                .filter(movement -> movement.getIdempotencyKey() != null)
                .filter(movement -> !card.hasAppliedPaymentOperation(
                        movement.getIdempotencyKey()))
                .map(PaymentMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (card.getBalance().subtract(reservedCharges).compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Virtual card has insufficient balance.");
        }

        PaymentMovement intent = new PaymentMovement(
                key, card.getCardId(), PaymentMovementType.CHARGE, amount, null,
                LocalDateTime.now(), key, operationReference, PaymentMovementStatus.PENDING);
        intent = saveOperationIntent(intent);
        return finishCharge(intent, card, key, customerId.trim(), operationReference);
    }

    public BigDecimal getBalance(String cardId) {
        return findCard(cardId).getBalance();
    }

    @Override
    public synchronized void recharge(String administratorId, String cardId, BigDecimal amount) {
        recharge(UUID.randomUUID().toString(), administratorId, cardId, amount);
    }

    @Override
    public synchronized void recharge(
            String idempotencyKey, String administratorId, String cardId, BigDecimal amount) {
        requireAuthorizedAdministrator(administratorId);
        String key = requiredText(idempotencyKey, "Idempotency key");
        validatePositiveAmount(amount);
        VirtualCard card = findCard(cardId);
        if (!card.isActive()) {
            throw new InactiveCardException("Virtual card is inactive.");
        }
        validateCardBalance(card);

        PaymentMovement movement = movementRepository.findByIdempotencyKey(key).orElse(null);
        if (movement != null) {
            validateSameOperation(movement, PaymentMovementType.RECHARGE, card.getCardId(),
                    amount, null, administratorId.trim());
        } else {
            movement = saveOperationIntent(new PaymentMovement(
                    key, card.getCardId(), PaymentMovementType.RECHARGE, amount, null,
                    LocalDateTime.now(), key, administratorId.trim(),
                    PaymentMovementStatus.PENDING));
        }

        if (movement.getStatus() == PaymentMovementStatus.PENDING) {
            try {
                paymentGateway.recharge(
                        key, administratorId.trim(), card.getCardId(), amount);
            } catch (RuntimeException gatewayFailure) {
                throw new PaymentGatewayException(
                        "Recharge outcome is unknown; retry with the same idempotency key.",
                        gatewayFailure);
            }
            movement = markExternalSuccess(movement, movement.getId(), LocalDateTime.now());
        }
        applyOperationBalance(card, movement, amount);
        markCompleted(movement);
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
        String key = requiredText(idempotencyKey, "Idempotency key");
        requireOriginalPayment(originalPayment);
        validatePositiveAmount(refundAmount);
        if (refundAmount.compareTo(originalPayment.getAmount()) > 0) {
            throw new PaymentDeclinedException("Refund cannot exceed the original payment.");
        }

        PaymentMovement priorRefund = movementRepository
                .findByIdempotencyKey(key)
                .orElse(null);
        if (priorRefund != null) {
            validateSameOperation(priorRefund, PaymentMovementType.REFUND,
                    originalPayment.getCardId(), refundAmount,
                    originalPayment.getTransactionId(), null);
            finishRefund(priorRefund, originalPayment, refundAmount, key);
            return;
        }

        PaymentMovement originalCharge = movementRepository.findByCardId(
                        originalPayment.getCardId()).stream()
                .filter(movement -> movement.getType() == PaymentMovementType.CHARGE)
                .filter(movement -> movement.getId().equals(originalPayment.getTransactionId()))
                .findFirst()
                .orElseThrow(() -> new PaymentDeclinedException(
                        "Original card charge was not found."));
        if (originalCharge.getAmount().compareTo(originalPayment.getAmount()) != 0) {
            throw new PaymentDeclinedException("Original payment data does not match the recorded charge.");
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

        PaymentMovement intent = saveOperationIntent(new PaymentMovement(
                key, card.getCardId(), PaymentMovementType.REFUND, refundAmount,
                originalPayment.getTransactionId(), LocalDateTime.now(), key, null,
                PaymentMovementStatus.PENDING));
        finishRefund(intent, originalPayment, refundAmount, key);
    }

    private PaymentReceipt finishCharge(
            PaymentMovement movement, VirtualCard card, String idempotencyKey,
            String customerId, String operationReference) {
        if (movement.getStatus() == PaymentMovementStatus.PENDING) {
            PaymentReceipt externalReceipt;
            try {
                externalReceipt = paymentGateway.charge(
                        idempotencyKey, operationReference,
                        card.getCardId(), customerId, movement.getAmount());
            } catch (RuntimeException gatewayFailure) {
                throw new PaymentGatewayException(
                        "Charge outcome is unknown; retry with the same idempotency key.",
                        gatewayFailure);
            }
            if (externalReceipt == null
                    || !card.getCardId().equals(externalReceipt.getCardId())
                    || externalReceipt.getAmount().compareTo(movement.getAmount()) != 0
                    || externalReceipt.getTransactionId() == null
                    || externalReceipt.getTransactionId().isBlank()) {
                throw new PaymentGatewayException(
                        "Gateway returned an invalid charge receipt; retry with the same idempotency key.",
                        new IllegalStateException("Invalid gateway receipt."));
            }
            movement = markExternalSuccess(
                    movement, externalReceipt.getTransactionId(), externalReceipt.getPaidAt());
        }
        applyOperationBalance(card, movement, movement.getAmount().negate());
        movement = markCompleted(movement);
        return new PaymentReceipt(movement.getId(), movement.getCardId(),
                movement.getAmount(), movement.getOccurredAt());
    }

    private void finishRefund(
            PaymentMovement movement, PaymentReceipt originalPayment,
            BigDecimal refundAmount, String idempotencyKey) {
        VirtualCard card = findCard(originalPayment.getCardId());
        if (movement.getStatus() == PaymentMovementStatus.PENDING) {
            try {
                paymentGateway.refund(idempotencyKey, originalPayment, refundAmount);
            } catch (RuntimeException gatewayFailure) {
                throw new PaymentGatewayException(
                        "Refund outcome is unknown; retry with the same idempotency key.",
                        gatewayFailure);
            }
            movement = markExternalSuccess(movement, movement.getId(), LocalDateTime.now());
        }
        applyOperationBalance(card, movement, refundAmount);
        markCompleted(movement);
    }

    private PaymentMovement saveOperationIntent(PaymentMovement intent) {
        try {
            return movementRepository.save(intent);
        } catch (RuntimeException saveFailure) {
            PaymentMovement persisted = movementRepository
                    .findByIdempotencyKey(intent.getIdempotencyKey()).orElse(null);
            if (persisted != null) {
                validateSameOperation(persisted, intent.getType(), intent.getCardId(),
                        intent.getAmount(), intent.getRelatedChargeId(),
                        intent.getOperationReference());
                return persisted;
            }
            throw new PaymentPersistenceException(
                    "Payment operation could not be registered; no gateway operation was attempted.",
                    saveFailure);
        }
    }

    private PaymentMovement markExternalSuccess(
            PaymentMovement movement, String transactionId, LocalDateTime occurredAt) {
        PaymentMovement externalSuccess = movement.withState(
                transactionId, occurredAt, PaymentMovementStatus.EXTERNAL_SUCCEEDED);
        try {
            return movementRepository.save(externalSuccess);
        } catch (RuntimeException saveFailure) {
            PaymentMovement persisted = movementRepository
                    .findByIdempotencyKey(movement.getIdempotencyKey()).orElse(null);
            if (persisted != null
                    && persisted.getStatus().ordinal()
                    >= PaymentMovementStatus.EXTERNAL_SUCCEEDED.ordinal()) {
                return persisted;
            }
            throw new PaymentPersistenceException(
                    "Gateway succeeded but its result was not recorded; retry with the same idempotency key.",
                    saveFailure);
        }
    }

    private void applyOperationBalance(
            VirtualCard card, PaymentMovement movement, BigDecimal balanceChange) {
        if (card.hasAppliedPaymentOperation(movement.getIdempotencyKey())) return;
        VirtualCard updatedCard = card.withBalanceChange(
                movement.getIdempotencyKey(), balanceChange);
        try {
            cardRepository.save(updatedCard);
        } catch (RuntimeException balanceFailure) {
            VirtualCard persistedCard = cardRepository.findById(card.getCardId()).orElse(null);
            if (persistedCard != null
                    && persistedCard.hasAppliedPaymentOperation(movement.getIdempotencyKey())) {
                return;
            }
            throw new PaymentPersistenceException(
                    "Payment movement is recorded but the balance update failed; retry with the same idempotency key.",
                    balanceFailure);
        }
    }

    private PaymentMovement markCompleted(PaymentMovement movement) {
        PaymentMovement completed = movement.withState(
                movement.getId(), movement.getOccurredAt(), PaymentMovementStatus.COMPLETED);
        try {
            return movementRepository.save(completed);
        } catch (RuntimeException saveFailure) {
            PaymentMovement persisted = movementRepository
                    .findByIdempotencyKey(movement.getIdempotencyKey()).orElse(null);
            if (persisted != null && persisted.getStatus() == PaymentMovementStatus.COMPLETED) {
                return persisted;
            }
            throw new PaymentPersistenceException(
                    "Balance was updated but operation completion could not be recorded; retry with the same idempotency key.",
                    saveFailure);
        }
    }

    private static void validateSameOperation(
            PaymentMovement movement, PaymentMovementType type, String cardId,
            BigDecimal amount, String relatedChargeId, String operationReference) {
        if (movement.getType() != type
                || !movement.getCardId().equals(cardId)
                || movement.getAmount().compareTo(amount) != 0
                || !java.util.Objects.equals(movement.getRelatedChargeId(), relatedChargeId)
                || !java.util.Objects.equals(
                        movement.getOperationReference(), operationReference)) {
            throw new PaymentDeclinedException(
                    "Idempotency key was already used for a different operation.");
        }
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

    private static String requiredText(String value, String label) {
        requireText(value, label);
        return value.trim();
    }

    private void requireAuthorizedAdministrator(String administratorId) {
        requireText(administratorId, "Administrator id");
        if (!authorizedAdministratorIds.contains(administratorId.trim())) {
            throw new PaymentDeclinedException("Administrator is not authorized to recharge cards.");
        }
    }
}
