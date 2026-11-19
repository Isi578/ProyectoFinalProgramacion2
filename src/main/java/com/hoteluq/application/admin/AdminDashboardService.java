package com.hoteluq.application.admin;

import com.hoteluq.application.contract.CardRechargeUseCase;
import com.hoteluq.application.contract.HotelReservationAdministrationUseCase;
import com.hoteluq.application.contract.ServiceAdministrationUseCase;
import com.hoteluq.application.port.CustomerCatalog;
import com.hoteluq.application.port.ReservationRepository;
import com.hoteluq.application.port.RoomCatalog;
import com.hoteluq.application.promotion.Promotion;
import com.hoteluq.application.promotion.PercentageDiscount;
import com.hoteluq.application.promotion.PromotionService;
import com.hoteluq.application.reservation.CancellationResult;
import com.hoteluq.domain.model.AdditionalService;
import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.Reservation;
import com.hoteluq.domain.model.Room;
import com.hoteluq.domain.model.RoomType;
import com.hoteluq.infrastructure.memory.InMemoryRoomCatalog;
import java.util.List;
import com.hoteluq.domain.model.ServiceOption;
import com.hoteluq.domain.model.ServicePackage;
import com.hoteluq.domain.model.ServicePricingUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AdminDashboardService (servicio del panel administrativo).
 *
 * Implementa el patrón Facade.
 *
 * Reúne las operaciones administrativas
 * de los demás módulos.
 *
 * Se implementa las reglas de pagos preexistentes,
 * reservas, servicios ni promociones.
 *
 * IMPORTANTE:
 * Actualmente utiliza una autorización demostrativa.
 * Debe conectarse al inicio de sesión real del proyecto.
 */
public final class AdminDashboardService {

    // Identificador del administrador de demostración.
    private final String demoAdministratorId;
    private static final String DEMO_PASSWORD = "hoteluq123";
    private boolean authenticated;

    // Repositorios que contienen los datos del hotel.
    private final ReservationRepository reservations;
    private final RoomCatalog rooms;
    private final CustomerCatalog customers;

    // Contratos de los otros módulos.
    private final HotelReservationAdministrationUseCase hotelCancellation;
    private final CardRechargeUseCase cardRecharge;
    private final ServiceAdministrationUseCase services;
    private final PromotionService promotionList;

    /**
     * Constructor de AdminDashboardService.
     *
     * Recibe los objetos existentes que utilizará
     * el administrador para consultar y operar.
     */
    public AdminDashboardService(
            String demoAdministratorId,
            ReservationRepository reservations,
            RoomCatalog rooms,
            CustomerCatalog customers,
            HotelReservationAdministrationUseCase hotelCancellation,
            CardRechargeUseCase cardRecharge,
            ServiceAdministrationUseCase services,
            PromotionService promotionList) {

        this.demoAdministratorId = demoAdministratorId;
        this.reservations = reservations;
        this.rooms = rooms;
        this.customers = customers;
        this.hotelCancellation = hotelCancellation;
        this.cardRecharge = cardRecharge;
        this.services = services;
        this.promotionList = promotionList;
    }

    /**
     * requireAdministrator (exigir administrador).
     *
     * Verifica el identificador de demostración.
     *
     * No equivale a una autenticación real.
     */
    /**
     * Comprueba ID y contraseña sin cambiar la autenticación de los clientes.
     * Credenciales previstas únicamente para la exposición académica.
     */
    public synchronized void authenticate(String administratorId, String password) {
        authenticated = false;
        if (administratorId == null || password == null
                || !demoAdministratorId.equals(administratorId.trim())
                || !DEMO_PASSWORD.equals(password)) {
            throw new SecurityException("ID o contraseña de administrador incorrectos.");
        }
        authenticated = true;
    }

    /** Desbloqueo temporal, cancelado al salir de la pestaña principal. */
    public synchronized void logout() { authenticated = false; }

    public synchronized void requireAdministrator(String administratorId) {

        if (!authenticated || administratorId == null ||
                !demoAdministratorId.equals(administratorId.trim())) {

            throw new SecurityException(
                    "Identificador de administrador no autorizado "
                            + "(modo demostración)."
            );
        }
    }

    /**
     * listOperations (listar operaciones).
     *
     * Consulta clientes, habitaciones, reservas,
     * servicios y promociones.
     */
    public String listOperations(String administratorId) {

        requireAdministrator(administratorId);

        StringBuilder text = new StringBuilder();

        text.append("HOTELUQ | CONSULTA ADMINISTRATIVA\n");

        // Consultamos los clientes registrados.
        text.append("\nCLIENTES\n");

        for (Customer customer : customers.findAll()) {

            text.append("  ")
                    .append(customer.getId())
                    .append(" - ")
                    .append(customer.getFullName())
                    .append(" - ")
                    .append(customer.getEmail())
                    .append('\n');
        }

        // Consultamos las habitaciones activas.
        text.append("\nHABITACIONES ACTIVAS\n");

        for (Room room : rooms.findActiveRooms()) {

            text.append("  ")
                    .append(room)
                    .append('\n');
        }

        // Consultamos las reservas.
        text.append("\nRESERVAS\n");

        for (Reservation reservation : reservations.findAll()) {

            text.append("  ")
                    .append(reservation)
                    .append('\n');
        }

        // Consultamos los servicios adicionales.
        text.append("\nSERVICIOS\n");

        for (ServiceOption service : services.listServices()) {

            text.append("  ")
                    .append(service.getId())
                    .append(" - ")
                    .append(service.getName());

            if (service.isAvailable()) {
                text.append(" (activo)");
            } else {
                text.append(" (inactivo)");
            }

            text.append('\n');
        }

        // Consultamos las promociones.
        text.append("\nPROMOCIONES\n");

        for (Promotion promotion : promotionList.listPromotions()) {

            text.append("  ")
                    .append(promotion.getCode())
                    .append(" - ")
                    .append(promotion.getName());

            if (promotion.isActive()) {
                text.append(" (activa)");
            } else {
                text.append(" (inactiva)");
            }

            text.append('\n');
        }

        return text.toString();
    }

    /** Devuelve la información estructurada para las tablas administrativas. */
    public List<Customer> listCustomers(String administratorId) {
        requireAdministrator(administratorId);
        return customers.findAll().stream()
                .sorted((a,b) -> a.getId().compareTo(b.getId())).toList();
    }

    public List<Room> listRooms(String administratorId) {
        requireAdministrator(administratorId);
        if (rooms instanceof InMemoryRoomCatalog catalog) return catalog.findAll();
        return rooms.findActiveRooms();
    }

    public List<Reservation> listReservations(String administratorId) {
        requireAdministrator(administratorId);
        return reservations.findAll();
    }

    public List<ServiceOption> listServices(String administratorId) {
        requireAdministrator(administratorId);
        return services.listServices();
    }

    public List<Promotion> listPromotions(String administratorId) {
        requireAdministrator(administratorId);
        return promotionList.listPromotions();
    }

    /** Las operaciones de habitaciones permanecen protegidas por la sesión admin. */
    public Room saveRoom(String administratorId, String number, String type,
                         BigDecimal price, int capacity, boolean update) {
        requireAdministrator(administratorId);
        if (!(rooms instanceof InMemoryRoomCatalog catalog)) {
            throw new IllegalStateException("El catálogo de habitaciones no admite modificaciones.");
        }
        if (number == null || number.isBlank() || price == null || price.signum() <= 0
                || capacity <= 0 || type == null || type.isBlank()) {
            throw new IllegalArgumentException("Revisa número, tipo, precio y capacidad.");
        }
        String typeId = type.trim().toUpperCase(java.util.Locale.ROOT);
        if (!typeId.equals("ESTANDAR") && !typeId.equals("SUITE")) {
            throw new IllegalArgumentException("Selecciona tipo Estándar o Suite.");
        }
        Room room = new Room(number.trim(), new RoomType(typeId,
                typeId.equals("SUITE") ? "Suite" : "Estándar", price), capacity, true);
        if (update) {
            // Una reserva histórica conserva la habitación original que contrató.
            catalog.update(room);
        } else {
            catalog.add(room);
        }
        return room;
    }

    public void deactivateRoom(String administratorId, String number) {
        requireAdministrator(administratorId);
        if (!(rooms instanceof InMemoryRoomCatalog catalog)) {
            throw new IllegalStateException("El catálogo no permite cambios.");
        }
        Room current = catalog.findAll().stream()
                .filter(room -> room.getNumber().equals(number)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Habitación no encontrada."));
        catalog.update(new Room(current.getNumber(), current.getRoomType(),
                current.getCapacity(), false));
    }

    /** Disponibilidad para un período, no un estado permanente de la habitación. */
    public boolean available(String administratorId, String number,
                             LocalDateTime start, LocalDateTime end) {
        requireAdministrator(administratorId);
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("El período seleccionado no es válido.");
        }
        if (rooms.findActiveRoom(number).isEmpty()) return false;
        return reservations.findAll().stream()
                .filter(r -> r.getRoom().getNumber().equals(number))
                .filter(r -> r.getStatus() == com.hoteluq.domain.model.ReservationStatus.CONFIRMED)
                .noneMatch(r -> start.isBefore(r.getCheckOutAt())
                        && end.isAfter(r.getCheckInAt()));
    }

    /**
     * cancelByHotel (cancelar por el hotel).
     *
     * Solicita al módulo de reservas realizar
     * una cancelación administrativa.
     *
     * No calcula el reembolso aquí.
     */
    public CancellationResult cancelByHotel(
            String administratorId,
            String reservationId) {

        requireAdministrator(administratorId);

        return hotelCancellation.cancelByHotel(
                reservationId,
                LocalDateTime.now()
        );
    }

    /**
     * recharge (recargar).
     *
     * Solicita al módulo de pagos
     * realizar una recarga de tarjeta.
     */
    public void recharge(
            String administratorId,
            String cardId,
            BigDecimal amount) {

        requireAdministrator(administratorId);

        cardRecharge.recharge(
                administratorId,
                cardId,
                amount
        );
    }

    /**
     * addService (agregar servicio).
     *
     * Registra un nuevo servicio adicional.
     */
    public ServiceOption addService(
            String administratorId,
            String id,
            String name,
            BigDecimal price,
            ServicePricingUnit pricingUnit) {

        requireAdministrator(administratorId);

        AdditionalService newService = new AdditionalService(
                id,
                name,
                price,
                pricingUnit,
                true
        );

        return services.addService(newService);
    }

    /** Crea un paquete con servicios que ya figuran en el catálogo. */
    public ServiceOption addPackage(String administratorId, String id, String name,
                                    String componentsCsv) {
        requireAdministrator(administratorId);
        if (componentsCsv == null || componentsCsv.isBlank()) {
            throw new IllegalArgumentException("Escribe los códigos de los servicios del paquete.");
        }
        ServicePackage.Builder builder = ServicePackage.builder().id(id).name(name);
        for (String code : componentsCsv.split(",")) {
            String normalized = code.trim();
            ServiceOption component = services.listServices().stream()
                    .filter(option -> option.getId().equals(normalized))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Servicio inexistente: " + normalized));
            if (!component.isAvailable()) {
                throw new IllegalArgumentException("Servicio inactivo: " + normalized);
            }
            builder.add(component);
        }
        return services.addService(builder.build());
    }

    /**
     * updateService (actualizar servicio).
     *
     * Solicita actualizar un servicio existente.
     */
    public ServiceOption updateService(
            String administratorId,
            String id,
            String name,
            BigDecimal price,
            ServicePricingUnit pricingUnit) {

        requireAdministrator(administratorId);

        AdditionalService updatedService = new AdditionalService(
                id,
                name,
                price,
                pricingUnit,
                true
        );

        return services.updateService(updatedService);
    }

    /**
     * deactivateService (desactivar servicio).
     *
     * Evita nuevas contrataciones del servicio.
     */
    public void deactivateService(
            String administratorId,
            String id) {

        requireAdministrator(administratorId);

        services.deactivateService(id);
    }

    /**
     * createPromotion (crear promoción).
     *
     * Registra una promoción.
     *
     * Valida que el porcentaje sea mayor que cero
     * y que no supere el máximo del 50 %.
     */
    public void createPromotion(
            String administratorId,
            String code,
            BigDecimal percentage,
            LocalDateTime start,
            LocalDateTime end,
            int minimumNights) {

        requireAdministrator(administratorId);

        // Validamos el porcentaje.
        if (percentage == null ||
                percentage.signum() <= 0 ||
                percentage.compareTo(new BigDecimal("50")) > 0) {

            throw new IllegalArgumentException(
                    "El porcentaje debe estar entre 0 y 50."
            );
        }

        // Validamos la cantidad mínima de noches.
        if (minimumNights < 1) {

            throw new IllegalArgumentException(
                    "La promoción debe exigir mínimo una noche."
            );
        }

        // Convertimos el porcentaje a su equivalente decimal.
        BigDecimal discount = percentage.divide(
                new BigDecimal("100")
        );

        // Utilizamos el Builder del módulo de promociones.
        Promotion promotion = Promotion.builder(
                        code,
                        new PercentageDiscount(discount)
                )
                .validBetween(start, end)
                .minNights(minimumNights)
                .build();

        // Delegamos el registro al módulo correspondiente.
        promotionList.registerPromotion(promotion);
    }

    /**
     * activatePromotion (activar promoción).
     *
     * Permite que la promoción vuelva a utilizarse.
     */
    public void activatePromotion(
            String administratorId,
            String code) {

        requireAdministrator(administratorId);

        promotionList.activatePromotion(code);
    }

    /**
     * deactivatePromotion (desactivar promoción).
     *
     * Impide utilizar la promoción en futuras reservas.
     */
    public void deactivatePromotion(
            String administratorId,
            String code) {

        requireAdministrator(administratorId);

        promotionList.deactivatePromotion(code);
    }
}