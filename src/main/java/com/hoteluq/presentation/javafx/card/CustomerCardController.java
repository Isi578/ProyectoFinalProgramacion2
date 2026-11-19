package com.hoteluq.presentation.javafx.card;

import com.hoteluq.application.contract.CustomerVirtualCardUseCase;
import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.PaymentMovement;
import com.hoteluq.domain.model.PaymentMovementType;
import com.hoteluq.domain.model.VirtualCard;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Connects the customer's authenticated session to their card information. */
public final class CustomerCardController {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CustomerCardView view;
    private final CustomerVirtualCardUseCase cards;
    private String customerId;

    public CustomerCardController(CustomerCardView view, CustomerVirtualCardUseCase cards) {
        this.view = view;
        this.cards = cards;
    }

    public void initialize() {
        view.getRefreshButton().setOnAction(event -> refresh());
        view.getCopyCardNumberButton().setOnAction(event -> {
            boolean copied = view.copyCardNumberToClipboard();
            view.setStatusMessage(copied
                    ? "Número copiado. Ya puedes pegarlo en Administración."
                    : "No hay un número de tarjeta para copiar.");
        });
        view.getCardSelector().valueProperty().addListener(
                (observable, oldCard, newCard) -> showSelectedCard(newCard));
        endSession();
    }

    public void startSession(Customer customer) {
        customerId = customer.getId();
        view.setActionsDisabled(false);
        List<VirtualCard> ownedCards = cards.findCardsForCustomer(customerId);
        view.setCards(ownedCards, null);
        if (ownedCards.isEmpty()) {
            view.showCardDetails("No tienes una tarjeta virtual asignada.",
                    "Saldo: —", "Contacta a Administración para obtener una tarjeta.");
            return;
        }
        showSelectedCard(view.getCardSelector().getValue());
    }

    public void endSession() {
        customerId = null;
        view.clear();
        view.setActionsDisabled(true);
    }

    private void refresh() {
        if (customerId == null) return;
        String selectedId = selectedCardId();
        view.setCards(cards.findCardsForCustomer(customerId), selectedId);
        showSelectedCard(view.getCardSelector().getValue());
    }

    private void showSelectedCard(VirtualCard card) {
        if (card == null || customerId == null) {
            view.setCardNumber(null);
            return;
        }
        try {
            view.setCardNumber(card.getCardId());
            BigDecimal balance = cards.getCustomerCardBalance(customerId, card.getCardId());
            List<PaymentMovement> movements =
                    cards.getCustomerCardMovements(customerId, card.getCardId());
            String status = card.isActive() ? "Tarjeta activa" : "Tarjeta inactiva";
            String balanceText = "Saldo disponible: " + cop().format(balance) + " COP";
            view.showCardDetails(status, balanceText, formatMovements(movements));
        } catch (RuntimeException failure) {
            view.showCardDetails("No se pudo consultar la tarjeta.",
                    "Saldo: —", failure.getMessage());
        }
    }

    private String selectedCardId() {
        VirtualCard selected = view.getCardSelector().getValue();
        return selected == null ? null : selected.getCardId();
    }

    private static String formatMovements(List<PaymentMovement> movements) {
        if (movements.isEmpty()) return "Todavía no hay movimientos registrados.";
        return movements.stream()
                .map(movement -> movement.getOccurredAt().format(DATE_FORMAT)
                        + " | " + movementType(movement.getType())
                        + " | " + movementAmount(movement)
                        + " | " + movement.getStatus())
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("Todavía no hay movimientos registrados.");
    }

    private static String movementType(PaymentMovementType type) {
        return switch (type) {
            case CHARGE -> "Pago";
            case REFUND -> "Reembolso";
            case RECHARGE -> "Recarga";
        };
    }

    private static String movementAmount(PaymentMovement movement) {
        String sign = movement.getType() == PaymentMovementType.CHARGE ? "-" : "+";
        return sign + cop().format(movement.getAmount()) + " COP";
    }

    private static NumberFormat cop() {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));
    }
}
