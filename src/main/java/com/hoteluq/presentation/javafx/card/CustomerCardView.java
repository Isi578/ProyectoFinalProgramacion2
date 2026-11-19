package com.hoteluq.presentation.javafx.card;

import com.hoteluq.domain.model.VirtualCard;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

/** Customer-facing screen for viewing virtual card balances and activity. */
public final class CustomerCardView {
    private final VBox root = new VBox(14);
    private final ComboBox<VirtualCard> cardSelector = new ComboBox<>();
    private final TextField cardNumber = new TextField();
    private final Button copyCardNumber = new Button("Copiar número");
    private final Label cardStatus = new Label("Inicia sesión para consultar tu tarjeta.");
    private final Label balance = new Label("Saldo: —");
    private final Button refresh = new Button("Actualizar saldo y movimientos");
    private final TextArea movements = new TextArea();

    public CustomerCardView() {
        Label title = new Label("HotelUQ | Tarjeta virtual");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#173f35;");

        cardSelector.setPromptText("Selecciona una tarjeta");
        cardSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(VirtualCard card) {
                if (card == null) return "";
                return card.getCardId() + (card.isActive() ? " (activa)" : " (inactiva)");
            }

            @Override
            public VirtualCard fromString(String value) {
                return null;
            }
        });
        cardSelector.setMaxWidth(Double.MAX_VALUE);
        cardNumber.setEditable(false);
        cardNumber.setPromptText("El número aparecerá al seleccionar una tarjeta");
        cardNumber.setMaxWidth(Double.MAX_VALUE);
        HBox cardNumberRow = new HBox(8, cardNumber, copyCardNumber);

        balance.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#176b57;");
        movements.setEditable(false);
        movements.setWrapText(true);
        movements.setPrefRowCount(16);

        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color:#ecf5f2;");
        root.getChildren().addAll(title, new Label("Mis tarjetas"), cardSelector,
                new Label("Número de tarjeta (cópialo para una recarga administrativa)"),
                cardNumberRow,
                cardStatus, balance, refresh, new Label("Movimientos recientes"), movements);
    }

    public void setCards(List<VirtualCard> cards, String selectedCardId) {
        cardSelector.setItems(FXCollections.observableArrayList(cards));
        VirtualCard selected = cards.stream()
                .filter(card -> card.getCardId().equals(selectedCardId))
                .findFirst()
                .orElse(cards.isEmpty() ? null : cards.get(0));
        cardSelector.setValue(selected);
    }

    public void showCardDetails(String status, String formattedBalance, String movementText) {
        cardStatus.setText(status);
        balance.setText(formattedBalance);
        movements.setText(movementText);
    }

    public void setCardNumber(String value) {
        cardNumber.setText(value == null ? "" : value);
        copyCardNumber.setDisable(value == null || value.isBlank());
    }

    public void setStatusMessage(String message) {
        cardStatus.setText(message);
    }

    public boolean copyCardNumberToClipboard() {
        if (cardNumber.getText().isBlank()) return false;
        ClipboardContent content = new ClipboardContent();
        content.putString(cardNumber.getText());
        return Clipboard.getSystemClipboard().setContent(content);
    }

    public void clear() {
        cardSelector.getItems().clear();
        cardNumber.clear();
        copyCardNumber.setDisable(true);
        cardStatus.setText("Inicia sesión para consultar tu tarjeta.");
        balance.setText("Saldo: —");
        movements.clear();
    }

    public void setActionsDisabled(boolean disabled) {
        cardSelector.setDisable(disabled);
        refresh.setDisable(disabled);
        copyCardNumber.setDisable(disabled || cardNumber.getText().isBlank());
    }

    public Parent getRoot() { return root; }
    public ComboBox<VirtualCard> getCardSelector() { return cardSelector; }
    public Button getRefreshButton() { return refresh; }
    public Button getCopyCardNumberButton() { return copyCardNumber; }
}
