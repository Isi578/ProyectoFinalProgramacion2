package com.hoteluq.presentation.javafx.purchase;

import com.hoteluq.domain.model.ServiceOption;
import com.hoteluq.domain.model.VirtualCard;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Customer-facing screen for buying an additional service outside a reservation. */
public final class CustomerPurchaseView {
    private final VBox root = new VBox(12);
    private final ComboBox<ServiceOption> serviceSelector = new ComboBox<>();
    private final ComboBox<VirtualCard> cardSelector = new ComboBox<>();
    private final Spinner<Integer> nights = new Spinner<>(1, 30, 1);
    private final Spinner<Integer> guests = new Spinner<>(1, 6, 1);
    private final Label estimate = new Label("Estimado: —");
    private final Button purchase = new Button("Comprar servicio");
    private final Label message = new Label();

    public CustomerPurchaseView() {
        Label title = new Label("HotelUQ | Compras de servicios");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#173f35;");
        Label explanation = new Label("Compra un servicio sin asociarlo a una reserva. "
                + "La tarifa se calcula para las noches y huéspedes elegidos.");
        explanation.setWrapText(true);

        serviceSelector.setPromptText("Selecciona un servicio");
        serviceSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(ServiceOption service) {
                return service == null ? "" : service.getName();
            }

            @Override
            public ServiceOption fromString(String value) { return null; }
        });
        cardSelector.setPromptText("Selecciona una tarjeta activa");
        cardSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(VirtualCard card) {
                return card == null ? "" : card.getCardId();
            }

            @Override
            public VirtualCard fromString(String value) { return null; }
        });
        serviceSelector.setMaxWidth(Double.MAX_VALUE);
        cardSelector.setMaxWidth(Double.MAX_VALUE);
        nights.setEditable(false);
        guests.setEditable(false);
        estimate.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:#176b57;");
        message.setWrapText(true);

        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color:#ecf5f2;");
        root.getChildren().addAll(title, explanation,
                new Label("Servicio"), serviceSelector,
                new Label("Noches para el cálculo"), nights,
                new Label("Huéspedes para el cálculo"), guests,
                estimate, new Label("Pagar con"), cardSelector, purchase, message);
    }

    public void setOptions(List<ServiceOption> services, List<VirtualCard> cards) {
        String serviceId = serviceSelector.getValue() == null
                ? null : serviceSelector.getValue().getId();
        String cardId = cardSelector.getValue() == null
                ? null : cardSelector.getValue().getCardId();
        List<ServiceOption> activeServices = services.stream()
                .filter(ServiceOption::isAvailable).toList();
        List<VirtualCard> activeCards = cards.stream()
                .filter(VirtualCard::isActive).toList();
        serviceSelector.setItems(FXCollections.observableArrayList(activeServices));
        cardSelector.setItems(FXCollections.observableArrayList(activeCards));
        serviceSelector.setValue(activeServices.stream()
                .filter(service -> service.getId().equals(serviceId))
                .findFirst().orElse(activeServices.isEmpty() ? null : activeServices.get(0)));
        cardSelector.setValue(activeCards.stream()
                .filter(card -> card.getCardId().equals(cardId))
                .findFirst().orElse(activeCards.isEmpty() ? null : activeCards.get(0)));
        if (activeServices.isEmpty()) message.setText("No hay servicios disponibles actualmente.");
        else if (activeCards.isEmpty()) message.setText("No tienes una tarjeta activa para pagar.");
    }

    public void showEstimate(BigDecimal value) {
        estimate.setText("Estimado: " + NumberFormat.getNumberInstance(
                Locale.forLanguageTag("es-CO")).format(value) + " COP");
    }

    public void showMessage(String value) { message.setText(value == null ? "" : value); }

    public void clear() {
        serviceSelector.getItems().clear();
        cardSelector.getItems().clear();
        nights.getValueFactory().setValue(1);
        guests.getValueFactory().setValue(1);
        estimate.setText("Estimado: —");
        message.setText("");
    }

    public void setActionsDisabled(boolean disabled) {
        serviceSelector.setDisable(disabled);
        cardSelector.setDisable(disabled);
        nights.setDisable(disabled);
        guests.setDisable(disabled);
        purchase.setDisable(disabled);
    }

    public Parent getRoot() { return root; }
    public ComboBox<ServiceOption> getServiceSelector() { return serviceSelector; }
    public ComboBox<VirtualCard> getCardSelector() { return cardSelector; }
    public Spinner<Integer> getNights() { return nights; }
    public Spinner<Integer> getGuests() { return guests; }
    public Button getPurchaseButton() { return purchase; }
}
