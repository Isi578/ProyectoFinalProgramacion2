import com.hoteluq.application.admin.AdminDashboardService;
import com.hoteluq.application.demo.DemoDataLoader;
import com.hoteluq.application.loyalty.PointsPaymentPolicy;
import com.hoteluq.application.reporting.DailyFiscalService;
import com.hoteluq.application.reporting.HotelReportService;
import com.hoteluq.application.reservation.*;
import com.hoteluq.application.service.CustomerServicePurchaseService;
import com.hoteluq.domain.model.*;
import com.hoteluq.infrastructure.factory.*;
import com.hoteluq.reporting.*;
import com.hoteluq.domain.pricing.AccommodationPricingService;
import java.math.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Pruebas ejecutables con, sin interfaz gráfica. */
public final class HotelUQIntegrationSmoke {
    static int passed;
    static void check(boolean okay, String message) {
        if (!okay) throw new AssertionError(message);
        passed++;
        System.out.println("OK " + passed + ": " + message);
    }
    static void fails(Runnable action, String message) {
        boolean thrown = false;
        try { action.run(); } catch (RuntimeException error) { thrown = true; }
        check(thrown, message);
    }
    public static void main(String[] args) throws Exception {
        HotelComponents c = new InMemoryHotelComponentsFactory().createComponents();
        LoyaltyPromotionComponents l = new InMemoryLoyaltyPromotionFactory().create(c, Clock.systemDefaultZone());
        var pub = new ReservationEventPublisher();
        var history = new ActivityHistoryObserver();
        pub.subscribe(history);
        pub.subscribe(l.getLoyaltyObserver());
        var res = new ReservationApplicationService(c.getRoomCatalog(), c.getReservationRepository(),
                c.getServiceCatalog(), c.getCustomerCatalog(), c.getPaymentGateway(),
                l.getPromotionService(), new AccommodationPricingService(),
                new StandardCancellationPolicy(), new HotelCancellationPolicy(),
                pub, history, l.getLoyaltyService());
        Path fiscals = Files.createTempDirectory("hoteluq-fiscal-integration-");
        DailyFiscalService fiscal = new DailyFiscalService(c.getReservationRepository(),
                c.getPaymentMovementRepository(), fiscals);
        var reports = new HotelReportService(c.getReservationRepository(), c.getRoomCatalog(),
                c.getPaymentMovementRepository(), c.getCustomerCatalog(), l.getLoyaltyService(),
                res, c.getServicePurchases());
        var admin = new AdminDashboardService("admin", c.getReservationRepository(), c.getRoomCatalog(),
                c.getCustomerCatalog(), res, c.getCardRechargeUseCase(),
                l.getServiceAdministration(), l.getPromotionService());
        check(c.getCustomerCatalog().findAll().isEmpty(), "No se precargan usuarios antiguos");
        check(l.getLoyaltyService().getAvailablePoints("GUEST-ANA-001") == 0,
                "No quedan puntos antiguos precargados");
        fails(() -> admin.authenticate("admin", "malaclave"), "Administrador rechaza contraseña incorrecta");
        fails(() -> admin.listOperations("admin"), "Sin sesión no hay operaciones de administración");
        admin.authenticate("admin", "hoteluq123");
        check(admin.listOperations("admin").contains("CLIENTES"), "Administración auténtica con ID y contraseña");
        admin.logout();
        fails(() -> admin.listOperations("admin"), "Al cerrar sesión se bloquean las operaciones");
        admin.authenticate("admin", "hoteluq123");
        Path file = Path.of("datos-demostracion.csv");
        var loader = new DemoDataLoader(c, l.getLoyaltyService(), fiscal, file);
        String summary = loader.load();
        check(summary.contains("20 clientes"), "CSV de demostración cargado desde raíz");
        check(c.getCustomerCatalog().findAll().size() == 20, "Exactamente 20 clientes de prueba");
        check(c.getReservationRepository().findAll().size() == 20, "Exactamente 20 reservas de prueba");
        check(c.getServicePurchases().findAll().size() == 10, "Exactamente 10 compras independientes");
        check(c.getReservationRepository().findAll().stream().filter(r -> !r.getServices().isEmpty()).count() == 10,
                "Exactamente 10 reservas incluyen un servicio");
        var testCustomer = c.getCustomerCatalog().findAll().get(0);
        var testCard = c.getCustomerVirtualCardUseCase().findCardsForCustomer(testCustomer.getId()).get(0);
        check(testCard.getBalance().compareTo(new BigDecimal("8000000")) > 0,
                "Cada tarjeta de prueba recibió 10 millones antes de los cobros");
        check(c.getCustomerAccountUseCase().authenticate(testCustomer.getEmail(), "cliente123") != null,
                "Los clientes importados pueden iniciar sesión");
        fails(() -> c.getCustomerAccountUseCase().authenticate(testCustomer.getEmail(), "mal"),
                "La clave incorrecta del cliente no autentica");
        var from = LocalDate.now().minusDays(20).atStartOfDay();
        var to = LocalDate.now().atTime(23,59,59);
        for (ReportType type : ReportType.values()) {
            check(reports.generate(type, from, to) != null,
                    "Informe operativo disponible: " + type);
        }
        var report = reports.generate(ReportType.ADDITIONAL_SERVICES, from, to);
        check(report.getTotal().intValueExact() == 20,
                "Informe de servicios suma 10 en reserva + 10 independientes");
        var pointReport = reports.generate(ReportType.CUSTOMER_POINTS, from, to);
        check(pointReport.getTotal().signum() > 0,
                "Puntos de demostración aparecen en su fecha histórica");
        var occupancy = reports.generate(ReportType.OCCUPANCY, from, to);
        check(occupancy.getTotal().signum() > 0,
                "Ocupación histórica muestra estadías dentro de los últimos 30 días");
        var income = reports.generate(ReportType.REVENUE_BY_PERIOD, from, to);
        check(income.getTotal().signum() > 0, "Ingresos contienen cobros de días distintos");
        LocalDate sample = LocalDate.now().minusDays(3);
        String closed = fiscal.readOnly(sample);
        check(closed.contains("CERRADO") && closed.contains("Fecha fiscal: " + sample),
                "Fiscal histórico archivado en la fecha correcta");
        fails(() -> {
            try { loader.load(); } catch (java.io.IOException e) { throw new RuntimeException(e); }
        }, "El botón no duplica la precarga de la misma sesión");
        var fresh = c.getCustomerAccountUseCase().register("Cliente Nuevas Pruebas", "nuevo@demo.local", "cliente123");
        var freshCard = c.getCustomerVirtualCardUseCase().findCardsForCustomer(fresh.getId()).get(0);
        c.getCardRechargeUseCase().recharge("admin", freshCard.getCardId(), new BigDecimal("1000000"));
        var purch = new CustomerServicePurchaseService(c.getServiceCatalog(), c.getPaymentGateway(),
                c.getCustomerVirtualCardUseCase(), l.getLoyaltyService(), c.getServicePurchases());
        var first = purch.purchaseServices(fresh.getId(), freshCard.getCardId(),
                List.of("SPA", "PARKING"), 2,1,0);
        check(first.getAmount().compareTo(new BigDecimal("96000")) == 0,
                "Compra múltiple suma SPA 60000 + parking 2*18000");
        fails(() -> purch.purchaseServices(fresh.getId(), freshCard.getCardId(),
                List.of("SPA","SPA"), 1,1,0), "Servicios repetidos no se cobran dos veces");
        fails(() -> purch.purchaseServices(fresh.getId(), freshCard.getCardId(),
                List.of("SPA"), 1,1,5000), "No permite pagar con puntos inexistentes");
        // Agregamos puntos mediante compras elegibles con la misma regla real.
        l.getLoyaltyService().recordEligibleSpend(fresh.getId(), "GANANCIA-TEST", new BigDecimal("250000"));
        int startPoints = l.getLoyaltyService().getAvailablePoints(fresh.getId());
        check(startPoints == 500, "Se acumulan 500 puntos desde una reserva elegible");
        var mixed = purch.purchaseServices(fresh.getId(), freshCard.getCardId(),
                List.of("SPA"),1,1,200);
        check(mixed.getAmount().compareTo(new BigDecimal("50000")) == 0
                        && l.getLoyaltyService().getAvailablePoints(fresh.getId()) == 300,
                "Compra mixta usa 200 puntos y cobra 50000 COP");
        fails(() -> purch.purchaseServices(fresh.getId(), freshCard.getCardId(),
                        List.of("BREAKFAST"),1,1,500),
                "Compra con puntos insuficientes no descuenta saldo");
        // 25000 COP requieren 500 puntos: agregamos 200 puntos más.
        l.getLoyaltyService().recordEligibleSpend(fresh.getId(), "GANANCIA-TEST-2",
                new BigDecimal("100000"));
        var pointsOnly = purch.purchaseServices(fresh.getId(), freshCard.getCardId(),
                List.of("BREAKFAST"),1,1,500);
        check(pointsOnly.getAmount().signum() == 0
                        && l.getLoyaltyService().getAvailablePoints(fresh.getId()) == 0,
                "Compra completamente con puntos no genera cobro monetario");
        l.getLoyaltyService().recordEligibleSpend(fresh.getId(), "GANANCIA-TEST-3",
                new BigDecimal("500000"));
        int availableBefore = l.getLoyaltyService().getAvailablePoints(fresh.getId());
        LocalDateTime checkIn = LocalDate.now().plusDays(90).atTime(15, 0);
        LocalDateTime checkOut = checkIn.plusDays(2).withHour(11);
        ReservationRequest request = ReservationRequest.builder()
                .customerId(fresh.getId()).roomNumber("101")
                .checkInAt(checkIn).checkOutAt(checkOut).guestCount(1)
                .serviceIds(List.of("SPA", "PARKING"))
                .cardId(freshCard.getCardId()).pointsToSpend(300)
                .paymentOperationId(UUID.randomUUID().toString()).build();
        ReservationQuote quote = res.quoteReservation(request);
        check(quote.pointsValue().compareTo(new BigDecimal("15000")) == 0,
                "Cotización calcula puntos sin realizar ningún cobro");
        check(l.getLoyaltyService().getAvailablePoints(fresh.getId()) == availableBefore,
                "Consultar cotización no consume puntos");
        Reservation booked = res.createReservation(request);
        check(booked.getTotalPaid().compareTo(quote.cardDue()) == 0
                        && booked.getPointsUsed() == 300,
                "Crear reserva con puntos cobra solo el resto y guarda puntos gastados");
        fails(() -> res.createReservation(ReservationRequest.builder()
                        .customerId(fresh.getId()).roomNumber("101")
                        .checkInAt(checkIn).checkOutAt(checkOut).guestCount(1)
                        .serviceIds(List.of()).cardId(freshCard.getCardId())
                        .paymentOperationId(UUID.randomUUID().toString()).build()),
                "Bloquea reserva solapada para una misma habitación");
        var cancel = res.cancelByHotel(booked.getId(), LocalDateTime.now());
        check(cancel.getRefundAmount().compareTo(booked.getTotalPaid()) == 0,
                "Cancelación hotelera devuelve todo lo pagado en tarjeta");
        check(l.getLoyaltyService().getAvailablePoints(fresh.getId()) >= availableBefore,
                "Cancelación hotelera también devuelve los puntos usados");
        var badCard = c.getCustomerVirtualCardUseCase().findCardsForCustomer(testCustomer.getId()).get(0);
        fails(() -> purch.purchaseServices(fresh.getId(), badCard.getCardId(),
                        List.of("SPA"),1,1,0),
                "No permite utilizar tarjeta perteneciente a otro cliente");
        check(c.getServiceCatalog().findById("AGUA").isPresent()
                        && c.getServiceCatalog().findById("KIT_ASEO").isPresent(),
                "Productos de consumo aparecen en el catálogo comprable");
        var afterPurchases = reports.generate(ReportType.ADDITIONAL_SERVICES, from, to);
        check(afterPurchases.getTotal().intValueExact() == 24,
                "Informe suma también compras múltiples y compras enteramente con puntos");
        // Verificamos compensación cuando no hay dinero para cobrar con puntos.
        var notFunded = c.getCustomerAccountUseCase().register(
                "Sin Saldo", "sin-saldo@demo.local", "cliente123");
        var emptyCard = c.getCustomerVirtualCardUseCase()
                .findCardsForCustomer(notFunded.getId()).get(0);
        l.getLoyaltyService().recordEligibleSpend(notFunded.getId(), "PREMIO",
                new BigDecimal("100000"));
        int preserved = l.getLoyaltyService().getAvailablePoints(notFunded.getId());
        fails(() -> purch.purchaseServices(notFunded.getId(), emptyCard.getCardId(),
                        List.of("SPA"),1,1,10),
                "Compra rechazada sin saldo de tarjeta");
        check(l.getLoyaltyService().getAvailablePoints(notFunded.getId()) == preserved,
                "Compra rechazada no pierde puntos");
        LocalDateTime nextArrival = LocalDateTime.now().plusHours(50);
        var partial = ReservationRequest.builder().customerId(fresh.getId())
                .roomNumber("102").checkInAt(nextArrival)
                .checkOutAt(nextArrival.plusDays(1)).guestCount(1)
                .cardId(freshCard.getCardId()).serviceIds(List.of())
                .pointsToSpend(100)
                .paymentOperationId(UUID.randomUUID().toString()).build();
        // La cancelación parcial necesita puntos que aún no estén gastados.
        l.getLoyaltyService().recordEligibleSpend(fresh.getId(), "GANANCIA-TEST-4",
                new BigDecimal("100000"));
        int beforePartial = l.getLoyaltyService().getAvailablePoints(fresh.getId());
        Reservation in48h = res.createReservation(partial);
        var partialCancel = res.cancelReservation(in48h.getId(), LocalDateTime.now());
        check(partialCancel.getRefundAmount().compareTo(in48h.getTotalPaid()
                        .multiply(new BigDecimal("0.80")).setScale(2,RoundingMode.HALF_UP)) == 0,
                "Cancelación de huésped entre 24 y 72 horas devuelve 80 % del cargo");
        check(l.getLoyaltyService().getAvailablePoints(fresh.getId()) == beforePartial - 20,
                "Cancelación parcial devuelve 80 % de puntos usados (no el 100 %)");
        byte[] fileBefore = Files.readAllBytes(fiscals.resolve("fiscal-" + sample + ".txt"));
        check(fiscal.archiveDemonstrationDays(List.of(sample)) == 0
                        && Arrays.equals(fileBefore, Files.readAllBytes(fiscals.resolve(
                        "fiscal-" + sample + ".txt"))),
                "Los cierres fiscales históricos no se pueden sobrescribir");
        System.out.println("\nTOTAL PRUEBAS CORRECTAS: " + passed);
    }
}