package com.hoteluq.presentation;

import com.hoteluq.application.admin.AdminDashboardService;
import com.hoteluq.application.reporting.DailyFiscalService;
import com.hoteluq.application.reporting.HotelReportService;

import com.hoteluq.presentation.javafx.admin.AdminView;
import com.hoteluq.presentation.javafx.admin.AdminController;

import com.hoteluq.presentation.javafx.auth.CustomerAuthController;
import com.hoteluq.presentation.javafx.auth.CustomerAuthView;
import com.hoteluq.reporting.HotelReportQuery;

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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;

/**
 * HotelUqApplication (aplicación HotelUQ).
 *
 * Es la clase principal de JavaFX.
 *
 * Conecta las reservas y la administración.
 *
 * Todos los módulos comparten los mismos repositorios.
 *
 * También configura la ejecución automática
 * del cierre fiscal diario.
 */
public final class HotelUqApplication extends Application {

    // fiscalClock (reloj fiscal): ejecuta la revisión periódica.
    private Timeline fiscalClock;

    /**
     * start (iniciar).
     *
     * Prepara los servicios, controladores,
     * vistas y ventana principal.
     */
    @Override
    public void start(Stage stage) {

        // Creamos los componentes principales del hotel.
        HotelComponentsFactory componentsFactory =
                new InMemoryHotelComponentsFactory();

        HotelComponents components =
                componentsFactory.createComponents();

        // Creamos los servicios de puntos y promociones.
        // Clock se conserva aquí porque lo utiliza la fábrica existente.
        LoyaltyPromotionComponents loyaltyPromotion =
                new InMemoryLoyaltyPromotionFactory()
                        .create(
                                components,
                                Clock.systemDefaultZone()
                        );

        // Preparamos los eventos de reservas.
        ReservationEventPublisher eventPublisher =
                new ReservationEventPublisher();

        ActivityHistoryObserver historyObserver =
                new ActivityHistoryObserver();

        // Registramos los observadores existentes.
        eventPublisher.subscribe(historyObserver);

        eventPublisher.subscribe(
                new CustomerNotificationObserver()
        );

        eventPublisher.subscribe(
                loyaltyPromotion.getLoyaltyObserver()
        );

        eventPublisher.subscribe(
                new VoucherUsageObserver(
                        loyaltyPromotion.getPromotionService()
                )
        );

        // Creamos el servicio de reservas.
        ReservationUseCase service =
                new ReservationApplicationService(
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
                        historyObserver
                );

        // Preparamos la fachada de reservas.
        ReservationFacade facade =
                new ReservationFacade(service);

        // Construimos la vista de reservas.
        ReservationView view =
                new ReservationView();

        new ReservationController(
                view,
                facade
        ).initialize();

        /**
         * Creamos el servicio de reportes.
         *
         * Utilizamos los mismos repositorios
         * que emplea el módulo de reservas.
         */
        HotelReportQuery reports =
                new HotelReportService(
                        components.getReservationRepository(),
                        components.getRoomCatalog(),
                        components.getPaymentMovementRepository(),
                        components.getCustomerCatalog(),
                        loyaltyPromotion.getLoyaltyService(),
                        service
                );

        // Creamos el servicio fiscal.
        DailyFiscalService fiscal =
                new DailyFiscalService(
                        components.getReservationRepository(),
                        components.getPaymentMovementRepository(),
                        Path.of(
                                System.getProperty("user.home"),
                                ".hoteluq",
                                "fiscales"
                        )
                );

        // Creamos el servicio administrativo Facade.
        AdminDashboardService admin =
                new AdminDashboardService(
                        "ADMIN-HOTEL-001",
                        components.getReservationRepository(),
                        components.getRoomCatalog(),
                        components.getCustomerCatalog(),
                        (com.hoteluq.application.contract
                                .HotelReservationAdministrationUseCase) service,
                        components.getCardRechargeUseCase(),
                        loyaltyPromotion.getServiceAdministration(),
                        loyaltyPromotion.getPromotionService()
                );

        // Construimos la vista administrativa.
        AdminView adminView =
                new AdminView();

        // Conectamos su controlador.
        new AdminController(
                adminView,
                admin,
                reports,
                fiscal
        ).initialize();

        // Creamos la pestaña de reservas.
        Tab reservationsTab =
                new Tab(
                        "Reservas",
                        view.getRoot()
                );

        // Creamos la pestaña administrativa.
        Tab adminTab =
                new Tab(
                        "Administración",
                        adminView.getRoot()
                );

        // Construimos la vista de acceso de clientes.
        CustomerAuthView authView =
                new CustomerAuthView();

        // Creamos la pestaña de cuenta de cliente.
        Tab accountTab =
                new Tab(
                        "Cuenta de cliente",
                        authView.getRoot()
                );

        accountTab.setClosable(false);
        reservationsTab.setClosable(false);
        adminTab.setClosable(false);

        // Unimos las pestañas.
        TabPane mainTabs =
                new TabPane(
                        accountTab,
                        reservationsTab,
                        adminTab
                );

        // Conectamos el acceso de clientes con las reservas:
        // al ingresar, se copia el ID del cliente y se abre
        // la pestaña de reservas.
        new CustomerAuthController(
                authView,
                components.getCustomerAccountUseCase(),
                customer -> {

                    view.getCustomerId().setText(
                            customer.getId()
                    );

                    mainTabs.getSelectionModel()
                            .select(reservationsTab);
                }
        ).initialize();

        /**
         * closeFiscalDay (cerrar día fiscal).
         *
         * Ejecuta la comprobación del informe
         * correspondiente al día anterior.
         */
        Runnable closeFiscalDay = () -> {

            try {

                fiscal.closeYesterdayAutomatically();

            } catch (IOException error) {

                System.err.println(
                        "No se pudo archivar el informe fiscal diario: "
                                + error.getMessage()
                );
            }
        };

        // Ejecutamos una comprobación al iniciar.
        closeFiscalDay.run();

        // Configuramos una revisión cada minuto.
        fiscalClock = new Timeline(
                new KeyFrame(
                        Duration.minutes(1),
                        event -> closeFiscalDay.run()
                )
        );

        fiscalClock.setCycleCount(
                Timeline.INDEFINITE
        );

        fiscalClock.play();

        // Configuramos la ventana principal.
        stage.setTitle(
                "HotelUQ - Reservas y administración"
        );

        stage.setScene(
                new Scene(
                        mainTabs,
                        1190,
                        770
                )
        );

        // Mostramos la ventana.
        stage.show();
    }

    /**
     * stop (detener).
     *
     * Detiene el reloj fiscal al cerrar
     * la aplicación.
     */
    @Override
    public void stop() {

        if (fiscalClock != null) {

            fiscalClock.stop();
        }
    }

    /**
     * main (principal).
     *
     * Punto de entrada de la aplicación.
     */
    public static void main(String[] args) {

        launch(args);
    }
}