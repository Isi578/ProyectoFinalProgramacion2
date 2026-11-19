package com.hoteluq.application.demo;

import com.hoteluq.application.loyalty.LoyaltyService;
import com.hoteluq.domain.loyalty.BenefitType;
import com.hoteluq.domain.loyalty.LoyaltyBenefit;
import com.hoteluq.application.port.ServiceStore;
import com.hoteluq.application.reporting.DailyFiscalService;
import com.hoteluq.domain.model.*;
import com.hoteluq.infrastructure.factory.HotelComponents;
import com.hoteluq.infrastructure.memory.InMemoryRoomCatalog;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Precargador EXCLUSIVAMENTE MANUAL para la demostración del profesor.
 *
 * Lee datos-demostracion.csv, ubicado junto al pom.xml. Todas las personas
 * y los pagos son ficticios. Importa clientes, habitaciones, reservas y compras.
 *
 * Para mostrar los informes diarios, las fechas de los movimientos importados
 * se ajustan a las fechas indicadas en el archivo; las tarjetas sí reciben
 * recargas y cargos verdaderos dentro de los repositorios en memoria.
 */
public final class DemoDataLoader {
    private final HotelComponents hotel;
    private final LoyaltyService loyalty;
    private final DailyFiscalService fiscal;
    private final Path file;
    private boolean attempted;

    public DemoDataLoader(HotelComponents hotel, LoyaltyService loyalty,
                          DailyFiscalService fiscal, Path file) {
        this.hotel = hotel;
        this.loyalty = loyalty;
        this.fiscal = fiscal;
        this.file = file;
    }

    public synchronized String load() throws IOException {
        if (attempted) {
            throw new IllegalStateException("La precarga solo puede ejecutarse una vez por sesión.");
        }
        if (!(hotel.getRoomCatalog() instanceof InMemoryRoomCatalog roomStore)
                || !(hotel.getServiceCatalog() instanceof ServiceStore)) {
            throw new IllegalStateException("La demostración necesita catálogos en memoria.");
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("No aparece " + file.toAbsolutePath() + " junto al pom.xml.");
        }

        List<String[]> commands = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.strip().startsWith("#")) continue;
            String[] data = line.split(";", -1);
            int expected = switch (data[0]) {
                case "HABITACION" -> 5;
                case "SERVICIO" -> 5;
                case "PAQUETE" -> 4;
                case "BENEFICIO" -> 6;
                case "CLIENTE" -> 4;
                case "RESERVA" -> 7;
                case "COMPRA" -> 6;
                default -> throw new IllegalArgumentException("Fila desconocida: " + line);
            };
            if (data.length != expected) {
                throw new IllegalArgumentException("Columnas inválidas: " + line);
            }
            commands.add(data);
        }
        long nClients = commands.stream().filter(a -> a[0].equals("CLIENTE")).count();
        long nReservations = commands.stream().filter(a -> a[0].equals("RESERVA")).count();
        long nPurchases = commands.stream().filter(a -> a[0].equals("COMPRA")).count();
        if (nClients != 20 || nReservations != 20 || nPurchases != 10) {
            throw new IllegalArgumentException(
                    "El archivo debe definir 20 clientes, 20 reservas y 10 compras independientes.");
        }
        long nRooms = commands.stream().filter(a -> a[0].equals("HABITACION")).count();
        long nServices = commands.stream().filter(a -> a[0].equals("SERVICIO")).count();
        long nPackages = commands.stream().filter(a -> a[0].equals("PAQUETE")).count();
        if (nRooms != 10 || nServices != 5 || nPackages != 1) {
            throw new IllegalArgumentException("El CSV debe contener 10 habitaciones, 5 servicios y un paquete.");
        }
        if (!hotel.getCustomerCatalog().findAll().isEmpty()
                || !hotel.getRoomCatalog().findActiveRooms().isEmpty()
                || !hotel.getServiceCatalog().findAll().isEmpty()
                || !hotel.getReservationRepository().findAll().isEmpty()) {
            throw new IllegalStateException("Para precargar, utiliza una ejecución limpia del hotel.");
        }
        attempted = true;
        List<Customer> customers = new ArrayList<>();
        List<String> cardIds = new ArrayList<>();
        Set<LocalDate> fiscalDays = new HashSet<>();
        int roomCount = 0;
        int reservationServices = 0;

        // Primera etapa: crear servicios, paquetes y beneficios desde este archivo.
        // Nunca se crean automáticamente al iniciar la aplicación.
        ServiceStore catalog = (ServiceStore) hotel.getServiceCatalog();
        int serviceCount = 0;
        int benefitCount = 0;
        for (String[] row : commands) {
            if (row[0].equals("SERVICIO")) {
                catalog.save(new AdditionalService(row[1], row[2], new BigDecimal(row[3]),
                        ServicePricingUnit.valueOf(row[4]), true));
                serviceCount++;
            }
        }
        for (String[] row : commands) {
            if (row[0].equals("PAQUETE")) {
                ServicePackage.Builder packageBuilder = ServicePackage.builder()
                        .id(row[1]).name(row[2]);
                for (String component : row[3].split(",")) {
                    packageBuilder.add(catalog.findById(component.trim()).orElseThrow(
                            () -> new IllegalArgumentException("Servicio desconocido: " + component)));
                }
                catalog.save(packageBuilder.build());
            }
            if (row[0].equals("BENEFICIO")) {
                loyalty.addDemonstrationBenefit(new LoyaltyBenefit(row[1], row[2],
                        BenefitType.valueOf(row[3]), Integer.parseInt(row[4]),
                        new BigDecimal(row[5])));
                benefitCount++;
            }
        }

        // Segunda etapa: habitaciones y clientes de prueba (10 millones por tarjeta).
        for (String[] row : commands) {
            if (row[0].equals("HABITACION")) {
                String type = row[2];
                RoomType roomType = new RoomType(type,
                        type.equals("SUITE") ? "Suite" : "Estándar",
                        new BigDecimal(row[3]));
                roomStore.add(new Room(row[1], roomType, Integer.parseInt(row[4]), true));
                roomCount++;
            }
            if (row[0].equals("CLIENTE")) {
                Customer customer = hotel.getCustomerAccountUseCase()
                        .register(row[1], row[2], row[3]);
                String card = hotel.getCustomerVirtualCardUseCase()
                        .findCardsForCustomer(customer.getId()).get(0).getCardId();
                // Esta recarga es de demostración: NO representa un ingreso por reserva.
                String key = "DEMO-RECARGA-" + customers.size();
                hotel.getCardRechargeUseCase().recharge(key, "admin", card,
                        new BigDecimal("10000000"));
                movePaymentDate(key, LocalDate.now().minusDays(12).atTime(9, 0));
                customers.add(customer);
                cardIds.add(card);
            }
        }

        // Segunda etapa: reservas pagadas y servicios adicionales en diez de ellas.
        // Incluimos estadías pasadas y futuras, sin solapar habitaciones.
        int index = 0;
        for (String[] row : commands) {
            if (!row[0].equals("RESERVA")) continue;
            int customerNumber = Integer.parseInt(row[1]);
            Customer customer = customers.get(customerNumber);
            Room room = roomStore.findActiveRoom(row[2]).orElseThrow();
            LocalDateTime checkIn = LocalDate.now().plusDays(Long.parseLong(row[3]))
                    .atTime(15, 0);
            LocalDateTime checkOut = checkIn.plusDays(Long.parseLong(row[4]))
                    .withHour(11);
            List<ServiceOption> selected = new ArrayList<>();
            if (!row[5].equals("NINGUNO")) {
                selected.add(hotel.getServiceCatalog().findById(row[5]).orElseThrow());
                reservationServices++;
            }
            int guests = 1;
            long nights = Long.parseLong(row[4]);
            BigDecimal total = room.getRoomType().getNightlyRate()
                    .multiply(BigDecimal.valueOf(nights));
            for (ServiceOption service : selected) {
                total = total.add(service.priceFor(nights, guests));
            }
            String key = "DEMO-RESERVA-" + index++;
            PaymentReceipt original = hotel.getPaymentGateway()
                    .charge(key, "RESERVA-DEMO:" + row[2], cardIds.get(customerNumber),
                            customer.getId(), total);
            LocalDateTime paymentDate = demonstrationDay(row[6]);
            fiscalDays.add(paymentDate.toLocalDate());
            movePaymentDate(key, paymentDate);
            PaymentReceipt dated = new PaymentReceipt(original.getTransactionId(),
                    original.getCardId(), original.getAmount(), paymentDate);
            Reservation reservation = new Reservation(customer, room, checkIn, checkOut,
                    guests, selected, total, dated);
            reservation.confirm();
            hotel.getReservationRepository().save(reservation);
            // El informe de puntos utiliza el mismo día histórico del pago.
            loyalty.recordDemonstrationSpend(customer.getId(), reservation.getId(),
                    total, paymentDate);
        }

        // Tercera etapa: diez compras por fuera de cualquier reserva.
        index = 0;
        for (String[] row : commands) {
            if (!row[0].equals("COMPRA")) continue;
            int customerNumber = Integer.parseInt(row[1]);
            Customer customer = customers.get(customerNumber);
            ServiceOption service = hotel.getServiceCatalog().findById(row[2]).orElseThrow();
            long nights = Long.parseLong(row[3]);
            int guests = Integer.parseInt(row[4]);
            BigDecimal total = service.priceFor(nights, guests);
            String key = "DEMO-COMPRA-" + index++;
            hotel.getPaymentGateway().charge(key,
                    "COMPRA-SERVICIOS:" + service.getId(), cardIds.get(customerNumber),
                    customer.getId(), total);
            LocalDateTime date = demonstrationDay(row[5]);
            movePaymentDate(key, date);
            fiscalDays.add(date.toLocalDate());
            hotel.getServicePurchases().save(new ServicePurchase(key, customer.getId(),
                    List.of(service.getId()), List.of(service.getName()),
                    total, total, 0, date));
        }

        int archived = fiscal.archiveDemonstrationDays(new ArrayList<>(fiscalDays));
        return "Precarga exitosa desde " + file.getFileName()
                + ": " + customers.size() + " clientes (10.000.000 COP cada uno), "
                + roomCount + " habitaciones, " + serviceCount + " servicios, "
                + benefitCount + " beneficios, 20 reservas, "
                + reservationServices + " servicios incluidos en reservas, 10 compras independientes. "
                + archived + " cierres fiscales históricos nuevos. "
                + "Consultar informes entre "
                + fiscalDays.stream().min(LocalDate::compareTo).orElse(LocalDate.now())
                + " y " + fiscalDays.stream().max(LocalDate::compareTo).orElse(LocalDate.now())
                + ". Clave de los clientes: cliente123.";
    }

    /** Mueve únicamente la fecha del movimiento sintético ya cobrado. */
    private void movePaymentDate(String key, LocalDateTime date) {
        PaymentMovement previous = hotel.getPaymentMovementRepository()
                .findByIdempotencyKey(key).orElseThrow();
        hotel.getPaymentMovementRepository().save(new PaymentMovement(
                previous.getId(), previous.getCardId(), previous.getType(),
                previous.getAmount(), previous.getRelatedChargeId(), date,
                previous.getIdempotencyKey(), previous.getOperationReference(),
                previous.getStatus()));
    }

    private LocalDateTime demonstrationDay(String relativeDays) {
        int offset = Integer.parseInt(relativeDays);
        if (offset > -2 || offset < -28) {
            throw new IllegalArgumentException("El pago de muestra debe ser entre hace 2 y 28 días.");
        }
        return LocalDate.now().plusDays(offset).atTime(LocalTime.NOON);
    }
}
