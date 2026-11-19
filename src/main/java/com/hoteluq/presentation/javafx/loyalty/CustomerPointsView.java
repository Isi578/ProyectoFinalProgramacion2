package com.hoteluq.presentation.javafx.loyalty;

import com.hoteluq.domain.loyalty.LoyaltyBenefit;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

/** Customer-facing view of earned points, benefits, and redemption vouchers. */
public final class CustomerPointsView {
    private final VBox root = new VBox(12);
    private final Label pointsBalance = new Label("Puntos disponibles: —");
    private final ComboBox<LoyaltyBenefit> benefitSelector = new ComboBox<>();
    private final Label benefitDetails = new Label("Selecciona un beneficio.");
    private final Button redeem = new Button("Canjear puntos");
    private final Button refresh = new Button("Actualizar puntos e historial");
    private final TextField voucherCode = new TextField();
    private final Button copyVoucher = new Button("Copiar código");
    private final TextArea history = new TextArea();
    private final Label message = new Label();

    public CustomerPointsView() {
        Label title = new Label("HotelUQ | Puntos y beneficios");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#173f35;");
        pointsBalance.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#176b57;");

        benefitSelector.setPromptText("Selecciona un beneficio");
        benefitSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(LoyaltyBenefit benefit) {
                return benefit == null ? "" : benefit.name() + " — "
                        + benefit.pointsCost() + " puntos";
            }

            @Override
            public LoyaltyBenefit fromString(String value) { return null; }
        });
        benefitSelector.setMaxWidth(Double.MAX_VALUE);

        voucherCode.setEditable(false);
        voucherCode.setPromptText("El código canjeado aparecerá aquí");
        voucherCode.setMaxWidth(Double.MAX_VALUE);
        copyVoucher.setDisable(true);

        HBox voucherRow = new HBox(8, voucherCode, copyVoucher);
        history.setEditable(false);
        history.setWrapText(true);
        history.setPrefRowCount(12);

        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color:#ecf5f2;");
        root.getChildren().addAll(title, pointsBalance, refresh,
                new Label("Beneficios disponibles"), benefitSelector, benefitDetails,
                redeem, new Label("Código de canje para una reserva"), voucherRow,
                message, new Label("Historial de puntos"), history);
    }

    public void setBenefits(List<LoyaltyBenefit> benefits, String selectedId) {
        benefitSelector.setItems(FXCollections.observableArrayList(benefits));
        LoyaltyBenefit selected = benefits.stream()
                .filter(benefit -> benefit.id().equals(selectedId))
                .findFirst()
                .orElse(benefits.isEmpty() ? null : benefits.get(0));
        benefitSelector.setValue(selected);
        showBenefitDetails(selected);
    }

    public void showPoints(int points, List<String> entries) {
        pointsBalance.setText("Puntos disponibles: " + points);
        history.setText(entries.isEmpty()
                ? "Todavía no hay movimientos de puntos."
                : String.join(System.lineSeparator(), entries));
    }

    public void showBenefitDetails(LoyaltyBenefit benefit) {
        if (benefit == null) {
            benefitDetails.setText("No hay beneficios disponibles.");
            return;
        }
        benefitDetails.setText(benefit.name() + " | Valor: "
                + benefit.value().toPlainString() + " COP | Costo: "
                + benefit.pointsCost() + " puntos");
    }

    public void showVoucher(String code, String status) {
        voucherCode.setText(code == null ? "" : code);
        copyVoucher.setDisable(code == null || code.isBlank());
        message.setText(status == null ? "" : status);
    }

    public void showMessage(String status) {
        message.setText(status == null ? "" : status);
    }

    public boolean copyVoucherToClipboard() {
        if (voucherCode.getText().isBlank()) return false;
        ClipboardContent content = new ClipboardContent();
        content.putString(voucherCode.getText());
        return Clipboard.getSystemClipboard().setContent(content);
    }

    public void clear() {
        pointsBalance.setText("Puntos disponibles: —");
        benefitSelector.getItems().clear();
        benefitDetails.setText("Inicia sesión para consultar beneficios.");
        voucherCode.clear();
        copyVoucher.setDisable(true);
        message.setText("");
        history.clear();
    }

    public void setActionsDisabled(boolean disabled) {
        benefitSelector.setDisable(disabled);
        redeem.setDisable(disabled);
        refresh.setDisable(disabled);
        copyVoucher.setDisable(disabled || voucherCode.getText().isBlank());
    }

    public Parent getRoot() { return root; }
    public ComboBox<LoyaltyBenefit> getBenefitSelector() { return benefitSelector; }
    public Button getRedeemButton() { return redeem; }
    public Button getRefreshButton() { return refresh; }
    public Button getCopyVoucherButton() { return copyVoucher; }
}
