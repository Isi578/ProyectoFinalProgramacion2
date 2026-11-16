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
import com.hoteluq.domain.pricing.AccommodationPricingService;
import com.hoteluq.infrastructure.factory.HotelComponents;
import com.hoteluq.infrastructure.factory.HotelComponentsFactory;
import com.hoteluq.infrastructure.factory.InMemoryHotelComponentsFactory;
import com.hoteluq.infrastructure.factory.InMemoryLoyaltyPromotionFactory;
import com.hoteluq.infrastructure.factory.LoyaltyPromotionComponents;
import com.hoteluq.presentation.javafx.reservation.ReservationController;
import com.hoteluq.presentation.javafx.reservation.ReservationView;
import javafx.application.Application;
import javafx.scene.Scene;
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
                historyObserver);

        ReservationFacade facade = new ReservationFacade(service);
        ReservationView view = new ReservationView();
        new ReservationController(view, facade).initialize();

        stage.setTitle("HotelUQ - Reservation Management");
        stage.setScene(new Scene(view.getRoot(), 920, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}