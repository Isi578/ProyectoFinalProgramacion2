package com.hoteluq.presentation.javafx.loyalty;

import com.hoteluq.application.contract.LoyaltyPointsUseCase;
import com.hoteluq.domain.model.Customer;

/** Connects points and benefits to the authenticated customer's account. */
public final class CustomerPointsController {
    private final CustomerPointsView view;
    private final LoyaltyPointsUseCase points;
    private String customerId;

    public CustomerPointsController(CustomerPointsView view, LoyaltyPointsUseCase points) {
        this.view = view;
        this.points = points;
    }

    public void initialize() {
        view.getRefreshButton().setOnAction(event -> refresh());
        view.getBenefitSelector().valueProperty().addListener(
                (observable, oldBenefit, newBenefit) -> view.showBenefitDetails(newBenefit));
        view.getRedeemButton().setOnAction(event -> redeem());
        view.getCopyVoucherButton().setOnAction(event -> view.showMessage(
                view.copyVoucherToClipboard()
                        ? "Código copiado. Pégalo en el campo de promoción al reservar."
                        : "No hay código para copiar."));
        endSession();
    }

    public void startSession(Customer customer) {
        customerId = customer.getId();
        view.setActionsDisabled(false);
        view.setBenefits(points.listBenefits(), null);
        refresh();
    }

    public void endSession() {
        customerId = null;
        view.clear();
        view.setActionsDisabled(true);
    }

    private void refresh() {
        if (customerId == null) return;
        view.showPoints(points.getAvailablePoints(customerId),
                points.getPointHistory(customerId));
        String selectedId = view.getBenefitSelector().getValue() == null
                ? null : view.getBenefitSelector().getValue().id();
        view.setBenefits(points.listBenefits(), selectedId);
    }

    private void redeem() {
        if (customerId == null) return;
        if (view.getBenefitSelector().getValue() == null) {
            view.showVoucher("", "Selecciona un beneficio para canjear.");
            return;
        }
        try {
            var benefit = view.getBenefitSelector().getValue();
            var voucher = points.redeemBenefit(customerId, benefit.id());
            view.showVoucher(voucher.getCode(), "Canje exitoso. Copia el código y úsalo en una reserva.");
            view.showPoints(points.getAvailablePoints(customerId),
                    points.getPointHistory(customerId));
        } catch (RuntimeException error) {
            view.showVoucher("", "No se pudo canjear: " + error.getMessage());
        }
    }
}
