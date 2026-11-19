package com.hoteluq.presentation.javafx.purchase;

import com.hoteluq.application.contract.CustomerServicePurchaseUseCase;
import com.hoteluq.application.contract.CustomerVirtualCardUseCase;
import com.hoteluq.application.contract.LoyaltyPointsUseCase;
import com.hoteluq.application.port.ServiceCatalog;
import com.hoteluq.domain.model.Customer;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Conecta la compra múltiple con la sesión, los pagos y los puntos. */
public final class CustomerPurchaseController {
    private final CustomerPurchaseView view;
    private final ServiceCatalog services;
    private final CustomerVirtualCardUseCase cards;
    private final CustomerServicePurchaseUseCase purchases;
    private final LoyaltyPointsUseCase loyalty;
    private String customerId;

    public CustomerPurchaseController(CustomerPurchaseView view, ServiceCatalog services,
                                      CustomerVirtualCardUseCase cards,
                                      CustomerServicePurchaseUseCase purchases,
                                      LoyaltyPointsUseCase loyalty) {
        this.view = view;
        this.services = services;
        this.cards = cards;
        this.purchases = purchases;
        this.loyalty = loyalty;
    }

    public void initialize() {
        view.getNights().valueProperty().addListener((obs, old, value) -> view.updateEstimate());
        view.getGuests().valueProperty().addListener((obs, old, value) -> view.updateEstimate());
        view.getPoints().valueProperty().addListener((obs, old, value) -> view.updateEstimate());
        view.getRefreshButton().setOnAction(event -> refreshCatalog());
        view.getPurchaseButton().setOnAction(event -> purchase());
        endSession();
    }

    public void startSession(Customer customer) {
        customerId = customer.getId();
        view.setActionsDisabled(false);
        refreshCatalog();
    }

    public void endSession() {
        customerId = null;
        view.clear();
        view.setActionsDisabled(true);
    }

    private void refreshCatalog() {
        if (customerId == null) return;
        view.setOptions(services.findAll(), cards.findCardsForCustomer(customerId));
        view.setAvailablePoints(loyalty.getAvailablePoints(customerId));
    }

    private void purchase() {
        if (customerId == null) return;
        try {
            List<String> selected = view.getSelectedServiceIds();
            var card = view.getCardSelector().getValue();
            if (selected.isEmpty() || card == null) {
                view.showMessage("Selecciona servicios y una tarjeta de tu cuenta.");
                return;
            }
            var receipt = purchases.purchaseServices(customerId, card.getCardId(), selected,
                    view.getNights().getValue(), view.getGuests().getValue(),
                    view.getPoints().getValue());
            int spent = view.getPoints().getValue();
            view.getPoints().getValueFactory().setValue(0);
            refreshCatalog();
            view.showMessage("Compra registrada. Cargo a tarjeta: "
                    + NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"))
                    .format(receipt.getAmount()) + " COP. Puntos utilizados: " + spent + ".");
        } catch (RuntimeException failure) {
            view.showMessage("No se pudo completar la compra: " + failure.getMessage());
        }
    }
}
