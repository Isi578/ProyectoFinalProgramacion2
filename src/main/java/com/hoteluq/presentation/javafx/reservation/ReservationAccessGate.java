package com.hoteluq.presentation.javafx.reservation;

import com.hoteluq.application.contract.CustomerAccountUseCase;
import com.hoteluq.presentation.javafx.card.CustomerCardController;
import com.hoteluq.presentation.javafx.loyalty.CustomerPointsController;
import com.hoteluq.presentation.javafx.purchase.CustomerPurchaseController;
import com.hoteluq.presentation.javafx.auth.CustomerAuthController;
import com.hoteluq.presentation.javafx.auth.CustomerAuthView;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * ReservationAccessGate (acceso a reservas).
 *
 * Conecta el inicio de sesión de clientes con las reservas.
 *
 * Mientras no haya sesión, la pestaña de reservas permanece
 * bloqueada. Al iniciar sesión se habilita y se abre; al cerrar
 * sesión se bloquea de nuevo y se vuelve a la cuenta.
 *
 * Lo usan todas las aplicaciones que muestran reservas.
 */
public final class ReservationAccessGate {

    private ReservationAccessGate() {
    }

    /**
     * connect (conectar).
     */
    public static void connect(
            TabPane tabs,
            Tab accountTab,
            Tab reservationsTab,
            CustomerAuthView authView,
            CustomerAccountUseCase accounts,
            ReservationController reservations,
            Tab cardTab,
            CustomerCardController cards,
            Tab pointsTab,
            CustomerPointsController points,
            Tab purchaseTab,
            CustomerPurchaseController purchases) {

        // Sin sesión, las reservas no se pueden abrir.
        reservationsTab.setDisable(true);
        cardTab.setDisable(true);
        pointsTab.setDisable(true);
        purchaseTab.setDisable(true);

        tabs.getSelectionModel().select(accountTab);

        new CustomerAuthController(
                authView,
                accounts,
                customer -> {

                    reservations.startSession(customer);
                    cards.startSession(customer);
                    points.startSession(customer);
                    purchases.startSession(customer);

                    reservationsTab.setDisable(false);
                    cardTab.setDisable(false);
                    pointsTab.setDisable(false);
                    purchaseTab.setDisable(false);

                    tabs.getSelectionModel().select(reservationsTab);
                },
                () -> {

                    reservations.endSession();
                    cards.endSession();
                    points.endSession();
                    purchases.endSession();

                    reservationsTab.setDisable(true);
                    cardTab.setDisable(true);
                    pointsTab.setDisable(true);
                    purchaseTab.setDisable(true);

                    tabs.getSelectionModel().select(accountTab);
                }
        ).initialize();
    }
}
