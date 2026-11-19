package com.hoteluq.presentation.javafx.purchase;

import com.hoteluq.domain.model.ServiceOption;
import com.hoteluq.domain.model.VirtualCard;
import com.hoteluq.application.loyalty.PointsPaymentPolicy;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vista de compras independientes de las reservas.
 *
 * Los CheckBox permiten combinar todos los servicios activos del catálogo.
 * Los puntos son voluntarios; el resumen muestra el pago mixto en COP.
 */
public final class CustomerPurchaseView {
    private final VBox root = new VBox(12);
    private final VBox serviceChoices = new VBox(5);
    private final Map<String, CheckBox> choices = new LinkedHashMap<>();
    private final Map<String, ServiceOption> options = new LinkedHashMap<>();
    private final ComboBox<VirtualCard> cardSelector = new ComboBox<>();
    private final Spinner<Integer> nights = new Spinner<>(1, 30, 1);
    private final Spinner<Integer> guests = new Spinner<>(1, 6, 1);
    private final Spinner<Integer> points = new Spinner<>(0, 1000000, 0, 10);
    private final Label availablePoints = new Label("Puntos disponibles: 0");
    private final Label estimate = new Label("Total: —");
    private final Button purchase = new Button("Comprar servicios seleccionados");
    private final Button refresh = new Button("Actualizar catálogo");
    private final Label message = new Label();
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));

    public CustomerPurchaseView() {
        Label title = new Label("HotelUQ | Compras de servicios");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#173f35;");
        Label explanation = new Label("Selecciona uno o varios servicios sin hacer una reserva. "
                + "También puedes cubrir parte o toda la compra con puntos.");
        explanation.setWrapText(true);
        cardSelector.setPromptText("Selecciona tu tarjeta virtual activa");
        cardSelector.setConverter(new StringConverter<>() {
            @Override public String toString(VirtualCard card) {
                return card == null ? "" : card.getCardId();
            }
            @Override public VirtualCard fromString(String value) { return null; }
        });
        cardSelector.setMaxWidth(Double.MAX_VALUE);
        ScrollPane scroll = new ScrollPane(serviceChoices);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(170);
        scroll.setStyle("-fx-background-color:white;");
        estimate.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#176b57;");
        message.setWrapText(true);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color:#ecf5f2;");
        root.getChildren().addAll(title, explanation,
                new Label("Servicios o paquetes disponibles"), scroll, refresh,
                new Label("Noches para calcular el precio"), nights,
                new Label("Huéspedes para calcular el precio"), guests,
                availablePoints, new Label("Puntos que quieres utilizar (0 = no usar)"), points,
                estimate, new Label("Tarjeta para el valor restante"), cardSelector,
                purchase, message);
    }

    /** Actualiza el catálogo sin perder las selecciones que siguen vigentes. */
    public void setOptions(List<ServiceOption> services, List<VirtualCard> cards) {
        List<String> selected = getSelectedServiceIds();
        String cardId = cardSelector.getValue() == null ? null : cardSelector.getValue().getCardId();
        choices.clear();
        options.clear();
        serviceChoices.getChildren().clear();
        List<ServiceOption> available = services.stream().filter(ServiceOption::isAvailable)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName())).toList();
        for (ServiceOption service : available) {
            CheckBox choice = new CheckBox();
            choice.setSelected(selected.contains(service.getId()));
            choice.setOnAction(event -> updateEstimate());
            options.put(service.getId(), service);
            choices.put(service.getId(), choice);
            serviceChoices.getChildren().add(choice);
        }
        if (available.isEmpty()) {
            serviceChoices.getChildren().add(new Label("No hay servicios activos. Administra el catálogo primero."));
        }
        List<VirtualCard> activeCards = cards.stream().filter(VirtualCard::isActive).toList();
        cardSelector.setItems(FXCollections.observableArrayList(activeCards));
        cardSelector.setValue(activeCards.stream().filter(c -> c.getCardId().equals(cardId))
                .findFirst().orElse(activeCards.isEmpty() ? null : activeCards.get(0)));
        updateEstimate();
    }

    public void setAvailablePoints(int balance) {
        availablePoints.setText("Puntos disponibles: " + balance);
    }

    public List<String> getSelectedServiceIds() {
        List<String> selected = new ArrayList<>();
        choices.forEach((id, checkbox) -> { if (checkbox.isSelected()) selected.add(id); });
        return selected;
    }

    public void updateEstimate() {
        BigDecimal total = BigDecimal.ZERO;
        for (String id : getSelectedServiceIds()) {
            total = total.add(options.get(id).priceFor(nights.getValue(), guests.getValue()));
        }
        int used = points.getValue();
        BigDecimal reduction = PointsPaymentPolicy.discount(used);
        BigDecimal remaining = total.subtract(reduction);
        choices.forEach((id, checkbox) -> checkbox.setText(options.get(id).getName() + " — "
                + money.format(options.get(id).priceFor(nights.getValue(), guests.getValue())) + " COP"));
        estimate.setText("Servicios: " + money.format(total) + " COP | Puntos: "
                + used + " (" + money.format(reduction) + " COP) | Tarjeta: "
                + money.format(remaining.max(BigDecimal.ZERO)) + " COP");
    }

    public void showMessage(String value) { message.setText(value == null ? "" : value); }
    public void clear() {
        choices.clear();
        options.clear();
        serviceChoices.getChildren().clear();
        cardSelector.getItems().clear();
        nights.getValueFactory().setValue(1);
        guests.getValueFactory().setValue(1);
        points.getValueFactory().setValue(0);
        setAvailablePoints(0);
        estimate.setText("Total: —");
        message.setText("");
    }
    public void setActionsDisabled(boolean disabled) {
        cardSelector.setDisable(disabled);
        nights.setDisable(disabled);
        guests.setDisable(disabled);
        points.setDisable(disabled);
        purchase.setDisable(disabled);
        refresh.setDisable(disabled);
        choices.values().forEach(choice -> choice.setDisable(disabled));
    }

    public Parent getRoot() { return root; }
    public ComboBox<VirtualCard> getCardSelector() { return cardSelector; }
    public Spinner<Integer> getNights() { return nights; }
    public Spinner<Integer> getGuests() { return guests; }
    public Spinner<Integer> getPoints() { return points; }
    public Button getPurchaseButton() { return purchase; }
    public Button getRefreshButton() { return refresh; }
}
