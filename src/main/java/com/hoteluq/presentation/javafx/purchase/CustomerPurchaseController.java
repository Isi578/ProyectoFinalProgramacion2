package com.hoteluq.presentation.javafx.purchase;

import com.hoteluq.application.contract.CustomerServicePurchaseUseCase;
import com.hoteluq.application.contract.CustomerVirtualCardUseCase;
import com.hoteluq.application.port.ServiceCatalog;
import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.ServiceOption;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Connects independent service purchases to the authenticated customer and their card. */
public final class CustomerPurchaseController {
    private final CustomerPurchaseView view;
    private final ServiceCatalog services;
    private final CustomerVirtualCardUseCase cards;
    private final CustomerServicePurchaseUseCase purchases;
    private String customerId;

    public CustomerPurchaseController(CustomerPurchaseView view, ServiceCatalog services,
                                     CustomerVirtualCardUseCase cards,
                                     CustomerServicePurchaseUseCase purchases) {
        this.view = view;
        this.services = services;
        this.cards = cards;
        this.purchases = purchases;
    }

    public void initialize() {
        view.getServiceSelector().valueProperty().addListener(
                (observable, oldValue, newValue) -> updateEstimate());
        view.getNights().valueProperty().addListener(
                (observable, oldValue, newValue) -> updateEstimate());
        view.getGuests().valueProperty().addListener(
                (observable, oldValue, newValue) -> updateEstimate());
        view.getPurchaseButton().setOnAction(event -> purchase());
        endSession();
    }

    public void startSession(Customer customer) {
        customerId = customer.getId();
        view.setActionsDisabled(false);
        refreshCatalog();
        updateEstimate();
    }

    public void endSession() {
        customerId = null;
        view.clear();
        view.setActionsDisabled(true);
    }

    private void refreshCatalog() {
        if (customerId == null) return;
        view.setOptions(services.findAll(), cards.findCardsForCustomer(customerId));
    }

    private void updateEstimate() {
        ServiceOption service = view.getServiceSelector().getValue();
        if (service == null) {
            view.showEstimate(BigDecimal.ZERO);
            return;
        }
        BigDecimal amount = service.priceFor(view.getNights().getValue(), view.getGuests().getValue());
        view.showEstimate(amount);
    }

    private void purchase() {
        if (customerId == null) return;
        try {
            refreshCatalog();
            ServiceOption service = view.getServiceSelector().getValue();
            var card = view.getCardSelector().getValue();
            if (service == null || card == null) {
                view.showMessage("Selecciona un servicio y una tarjeta activa.");
                return;
            }
            var receipt = purchases.purchaseService(customerId, card.getCardId(), service.getId(),
                    view.getNights().getValue(), view.getGuests().getValue());
            view.showMessage("Compra aprobada por " + NumberFormat.getNumberInstance(
                    Locale.forLanguageTag("es-CO")).format(receipt.getAmount())
                    + " COP. Revisa los movimientos en Tarjeta virtual.");
        } catch (RuntimeException failure) {
            view.showMessage("No se pudo completar la compra: " + failure.getMessage());
        }
    }
}
