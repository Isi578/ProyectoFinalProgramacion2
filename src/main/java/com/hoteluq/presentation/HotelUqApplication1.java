package com.hoteluq.presentation;

import com.hoteluq.application.promotion.VoucherUsageObserver;
import com.hoteluq.application.reservation.ActivityHistoryObserver;
import com.hoteluq.application.reservation.CustomerNotificationObserver;
import com.hoteluq.application.reservation.HotelCancellationPolicy;
import com.hoteluq.application.reservation.ReservationApplicationService;
import com.hoteluq.application.reservation.ReservationEventPublisher;
import com.hoteluq.application.reservation.ReservationFacade;
import com.hoteluq.application.reservation.ReservationUseCase;
import com.hoteluq.application.reservation.StandardCancellationPolicy;
import com.hoteluq.application.service.CustomerServicePurchaseService;
import com.hoteluq.domain.pricing.AccommodationPricingService;
import com.hoteluq.infrastructure.factory.HotelComponents;
import com.hoteluq.infrastructure.factory.HotelComponentsFactory;
import com.hoteluq.infrastructure.factory.InMemoryHotelComponentsFactory;
import com.hoteluq.infrastructure.factory.InMemoryLoyaltyPromotionFactory;
import com.hoteluq.infrastructure.factory.LoyaltyPromotionComponents;
import com.hoteluq.presentation.javafx.auth.CustomerAuthView;
import com.hoteluq.presentation.javafx.card.CustomerCardController;
import com.hoteluq.presentation.javafx.card.CustomerCardView;
import com.hoteluq.presentation.javafx.loyalty.CustomerPointsController;
import com.hoteluq.presentation.javafx.loyalty.CustomerPointsView;
import com.hoteluq.presentation.javafx.purchase.CustomerPurchaseController;
import com.hoteluq.presentation.javafx.purchase.CustomerPurchaseView;
import com.hoteluq.presentation.javafx.reservation.ReservationController;
import com.hoteluq.presentation.javafx.reservation.ReservationView;
import com.hoteluq.presentation.javafx.reservation.ReservationAccessGate;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.time.Clock;

public final class HotelUqApplication1 extends Application {
    @Override
    public void start(Stage stage) {
        HotelComponentsFactory componentsFactory = new InMemoryHotelComponentsFactory();
        HotelComponents components = componentsFactory.createComponents();
        LoyaltyPromotionComponents loyaltyPromotion = new InMemoryLoyaltyPromotionFactory()
                .create(components, Clock.systemDefaultZone());

        ReservationEventPublisher eventPublisher = new ReservationEventPublisher();
        ActivityHistoryObserver historyObserver = new ActivityHistoryObserver();
        eventPublisher.subscribe(historyObserver);
        eventPublisher.subscribe(new CustomerNotificationObserver());
        eventPublisher.subscribe(loyaltyPromotion.getLoyaltyObserver());
        eventPublisher.subscribe(new VoucherUsageObserver(loyaltyPromotion.getPromotionService()));

        ReservationUseCase service = new ReservationApplicationService(
                components.getRoomCatalog(),
                components.getReservationRepository(),
                components.getServiceCatalog(),
                components.getCustomerCatalog(),
                components.getPaymentGateway(),
                loyaltyPromotion.getPromotionService(),
                new AccommodationPricingService(),
                new StandardCancellationPolicy(),
                new HotelCancellationPolicy(),
                eventPublisher,
                historyObserver,
                loyaltyPromotion.getLoyaltyService());

        ReservationFacade facade = new ReservationFacade(service);
        ReservationView view = new ReservationView();
        ReservationController reservationController = new ReservationController(
                view, facade, components.getServiceCatalog(),
                components.getCustomerVirtualCardUseCase(),
                loyaltyPromotion.getLoyaltyService());
        reservationController.initialize();

        CustomerAuthView authView = new CustomerAuthView();
        CustomerCardView cardView = new CustomerCardView();
        CustomerCardController cardController = new CustomerCardController(
                cardView, components.getCustomerVirtualCardUseCase());
        cardController.initialize();
        Tab accountTab = new Tab("Cuenta de cliente", authView.getRoot());
        Tab reservationsTab = new Tab("Reservas", view.getRoot());
        Tab cardTab = new Tab("Tarjeta virtual", cardView.getRoot());
        CustomerPointsView pointsView = new CustomerPointsView();
        CustomerPointsController pointsController = new CustomerPointsController(
                pointsView, loyaltyPromotion.getLoyaltyService());
        pointsController.initialize();
        Tab pointsTab = new Tab("Puntos y beneficios", pointsView.getRoot());
        CustomerPurchaseView purchaseView = new CustomerPurchaseView();
        CustomerPurchaseController purchaseController = new CustomerPurchaseController(
                purchaseView,
                components.getServiceCatalog(),
                components.getCustomerVirtualCardUseCase(),
                new CustomerServicePurchaseService(
                        components.getServiceCatalog(), components.getPaymentGateway(),
                        components.getCustomerVirtualCardUseCase(),
                        loyaltyPromotion.getLoyaltyService(),
                        components.getServicePurchases()),
                loyaltyPromotion.getLoyaltyService());
        purchaseController.initialize();
        Tab purchaseTab = new Tab("Compras", purchaseView.getRoot());
        accountTab.setClosable(false);
        reservationsTab.setClosable(false);
        cardTab.setClosable(false);
        pointsTab.setClosable(false);
        purchaseTab.setClosable(false);
        TabPane tabs = new TabPane(accountTab, reservationsTab, cardTab, pointsTab, purchaseTab);

        // Las reservas solo se abren después de iniciar sesión.
        ReservationAccessGate.connect(
                tabs,
                accountTab,
                reservationsTab,
                authView,
                components.getCustomerAccountUseCase(),
                reservationController,
                cardTab,
                cardController,
                pointsTab,
                pointsController,
                purchaseTab,
                purchaseController);

        stage.setTitle("HotelUQ - Gestión de reservas");
        stage.setScene(new Scene(tabs, 920, 640));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}