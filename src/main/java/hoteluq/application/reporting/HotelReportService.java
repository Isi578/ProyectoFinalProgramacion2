package hoteluq.application.reporting;

import com.hoteluq.application.port.CustomerCatalog;
import com.hoteluq.application.port.LoyaltyHistoryQuery;
import com.hoteluq.application.port.PaymentMovementRepository;
import com.hoteluq.application.port.ReservationRepository;
import com.hoteluq.application.port.RoomCatalog;
import com.hoteluq.application.reservation.ReservationEvent;
import com.hoteluq.application.reservation.ReservationUseCase;
import com.hoteluq.domain.loyalty.LoyaltyEntry;
import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.PaymentMovement;
import com.hoteluq.domain.model.PaymentMovementStatus;
import com.hoteluq.domain.model.PaymentMovementType;
import com.hoteluq.domain.model.Reservation;
import com.hoteluq.domain.model.ReservationStatus;
import com.hoteluq.domain.model.Room;
import com.hoteluq.domain.model.ServiceOption;
import com.hoteluq.reporting.HotelReportQuery;
import com.hoteluq.reporting.ReportData;
import com.hoteluq.reporting.ReportPoint;
import com.hoteluq.reporting.ReportType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * HotelReportService (servicio de informes del hotel).
 *
 * Consulta la información de los demás módulos.
 *
 * No realiza reservas ni pagos.
 *
 * Utiliza bucles y condicionales para organizar y recorrer datos
 *
 * Implementa HotelReportQuery.
 */
public final class HotelReportService implements HotelReportQuery {

    private final ReservationRepository reservations;
    private final RoomCatalog rooms;
    private final PaymentMovementRepository movements;
    private final CustomerCatalog customers;
    private final LoyaltyHistoryQuery loyalty;
    private final ReservationUseCase reservationEvents;

    /**
     * Constructor de HotelReportService.
     *
     * Recibe los repositorios compartidos
     * con los otros módulos del hotel.
     */
    public HotelReportService(
            ReservationRepository reservations,
            RoomCatalog rooms,
            PaymentMovementRepository movements,
            CustomerCatalog customers,
            LoyaltyHistoryQuery loyalty,
            ReservationUseCase reservationEvents) {

        this.reservations = reservations;
        this.rooms = rooms;
        this.movements = movements;
        this.customers = customers;
        this.loyalty = loyalty;
        this.reservationEvents = reservationEvents;
    }

    /**
     * generate (generar).
     *
     * Recibe el tipo de informe y el período.
     *
     * Utiliza switch para seleccionar
     * el método que realizará el cálculo.
     */
    @Override
    public ReportData generate(
            ReportType type,
            LocalDateTime from,
            LocalDateTime to) {

        // Verificamos las fechas y el tipo de informe.
        if (type == null ||
                from == null ||
                to == null ||
                to.isBefore(from)) {

            throw new IllegalArgumentException(
                    "Seleccione un tipo y un período de fechas válido."
            );
        }

        // Seleccionamos el informe solicitado.
        switch (type) {

            case RESERVATIONS_BY_PERIOD:
                return byReservationPaymentDate(from, to);

            case MOST_BOOKED_ROOMS:
                return mostBookedRooms(from, to);

            case OCCUPANCY:
                return occupancy(from, to);

            case REVENUE_BY_PERIOD:
                return revenue(from, to);

            case ADDITIONAL_SERVICES:
                return services(from, to);

            case RECHARGES:
                return recharges(from, to);

            case CANCELLATIONS_AND_REFUNDS:
                return cancellations(from, to);

            case CUSTOMER_POINTS:
                return points(from, to);

            default:
                throw new IllegalArgumentException(
                        "Tipo de informe desconocido."
                );
        }
    }

    /**
     * byReservationPaymentDate (reservas por fecha de pago).
     *
     * Cuenta las reservas pagadas dentro del período.
     *
     * También organiza las cantidades por día
     * para generar gráficas.
     */
    private ReportData byReservationPaymentDate(
            LocalDateTime from,
            LocalDateTime to) {

        List<String> lines = new ArrayList<>();
        List<ReportPoint> bars = new ArrayList<>();

        int total = 0;

        // Recorremos todas las reservas.
        for (Reservation reservation : reservations.findAll()) {

            LocalDateTime date =
                    reservation.getPaymentReceipt().getPaidAt();

            // Ignoramos pagos fuera del período.
            if (!inside(date, from, to)) {
                continue;
            }

            total++;

            // Agregamos los datos de la reserva.
            lines.add(
                    date.toLocalDate()
                            + " | Habitación "
                            + reservation.getRoom().getNumber()
                            + " | Estado: "
                            + reservation.getStatus()
                            + " | Pago: $"
                            + money(reservation.getTotalPaid())
            );

            // Sumamos una reserva al día correspondiente.
            add(
                    bars,
                    date.toLocalDate().toString(),
                    BigDecimal.ONE
            );
        }

        return result(
                "Reservas realizadas (fecha de pago)",
                from,
                to,
                lines,
                BigDecimal.valueOf(total),
                bars,
                "reservas",
                total
        );
    }

    /**
     * mostBookedRooms (habitaciones más reservadas).
     *
     * Cuenta las reservas confirmadas
     * según su fecha de llegada.
     */
    private ReportData mostBookedRooms(
            LocalDateTime from,
            LocalDateTime to) {

        List<ReportPoint> totals = new ArrayList<>();

        // Recorremos las reservas.
        for (Reservation reservation : reservations.findAll()) {

            // Solo contamos reservas confirmadas del período.
            if (reservation.getStatus() == ReservationStatus.CONFIRMED
                    && inside(reservation.getCheckInAt(), from, to)) {

                add(
                        totals,
                        reservation.getRoom().getNumber(),
                        BigDecimal.ONE
                );
            }
        }

        // Ordenamos de mayor a menor.
        sortDescending(totals);

        List<String> lines = new ArrayList<>();

        int count = 0;

        // Construimos las filas del informe.
        for (ReportPoint point : totals) {

            lines.add(
                    "Habitación "
                            + point.getLabel()
                            + " | Reservas vigentes: "
                            + point.getValue()
            );

            count += point.getValue().intValue();
        }

        return result(
                "Habitaciones más reservadas (fecha de llegada)",
                from,
                to,
                lines,
                BigDecimal.valueOf(count),
                totals,
                "reservas",
                count
        );
    }

    /**
     * occupancy (ocupación).
     *
     * Calcula la ocupación por habitaciones-noche.
     *
     * Una habitación está ocupada durante las noches
     * comprendidas entre entrada y salida.
     *
     * La noche de salida no se cuenta.
     */
    private ReportData occupancy(
            LocalDateTime from,
            LocalDateTime to) {

        // Obtenemos únicamente las fechas.
        LocalDate start = from.toLocalDate();
        LocalDate end = to.toLocalDate();

        // Consultamos las habitaciones activas.
        List<Room> activeRooms = rooms.findActiveRooms();

        // Cantidad de días del período, incluyendo ambos extremos.
        long days =
                java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;

        // Capacidad total de habitaciones-noche.
        long capacity = days * activeRooms.size();

        // Cantidad de habitaciones-noche ocupadas.
        long occupied = 0;

        // Recorremos cada día.
        for (LocalDate day = start;
             !day.isAfter(end);
             day = day.plusDays(1)) {

            // Recorremos las habitaciones activas.
            for (Room room : activeRooms) {

                // Revisamos las reservas de cada habitación.
                for (Reservation reservation :
                        reservations.findByRoomNumber(room.getNumber())) {

                    // Ignoramos reservas no confirmadas.
                    if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
                        continue;
                    }

                    LocalDate checkIn =
                            reservation.getCheckInAt().toLocalDate();

                    LocalDate checkOut =
                            reservation.getCheckOutAt().toLocalDate();

                    // Revisamos si la habitación estaba ocupada ese día.
                    if (!day.isBefore(checkIn) && day.isBefore(checkOut)) {

                        occupied++;

                        // Evitamos contar dos veces la misma habitación.
                        break;
                    }
                }
            }
        }

        // Calculamos el porcentaje de ocupación.
        BigDecimal percentage = BigDecimal.ZERO;

        if (capacity > 0) {

            percentage = BigDecimal.valueOf(occupied)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            BigDecimal.valueOf(capacity),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        List<String> lines = new ArrayList<>();

        lines.add(
                "Habitaciones activas: " + activeRooms.size()
        );

        lines.add(
                "Noches consultadas: " + days
        );

        lines.add(
                "Capacidad: " + capacity + " habitaciones-noche"
        );

        lines.add(
                "Ocupadas: " + occupied + " habitaciones-noche"
        );

        lines.add(
                "Libres: " + (capacity - occupied) + " habitaciones-noche"
        );

        lines.add(
                "Ocupación promedio: " + percentage.toPlainString() + "%"
        );

        // Datos de la gráfica de ocupación.
        List<ReportPoint> chart = new ArrayList<>();

        chart.add(
                new ReportPoint(
                        "Ocupadas",
                        BigDecimal.valueOf(occupied)
                )
        );

        chart.add(
                new ReportPoint(
                        "Libres",
                        BigDecimal.valueOf(capacity - occupied)
                )
        );

        return result(
                "Ocupación hotelera (habitaciones-noche)",
                from,
                to,
                lines,
                percentage,
                chart,
                "%",
                activeRooms.size()
        );
    }

    /**
     * revenue (ingresos).
     *
     * Calcula los ingresos netos del hotel.
     *
     * Suma los cobros aprobados.
     * Resta los reembolsos.
     *
     * Las recargas NO se cuentan como ingresos.
     */
    private ReportData revenue(
            LocalDateTime from,
            LocalDateTime to) {

        List<String> lines = new ArrayList<>();
        List<ReportPoint> chart = new ArrayList<>();

        BigDecimal net = BigDecimal.ZERO;

        // Recorremos los movimientos.
        for (PaymentMovement movement : movements.findAll()) {

            // Ignoramos movimientos fuera del período.
            if (!completedWithin(movement, from, to)) {
                continue;
            }

            // Solo consideramos cobros y reembolsos.
            if (movement.getType() != PaymentMovementType.CHARGE
                    && movement.getType() != PaymentMovementType.REFUND) {

                continue;
            }

            BigDecimal amount = movement.getAmount();

            // Los reembolsos representan valores negativos.
            if (movement.getType() == PaymentMovementType.REFUND) {

                amount = amount.negate();
            }

            // Sumamos el movimiento al ingreso neto.
            net = net.add(amount);

            lines.add(
                    movement.getOccurredAt().toLocalDate()
                            + " | "
                            + movement.getType()
                            + " | $"
                            + money(amount)
            );

            // Agrupamos el movimiento por fecha.
            add(
                    chart,
                    movement.getOccurredAt().toLocalDate().toString(),
                    amount
            );
        }

        return result(
                "Ingresos netos (cobros menos reembolsos)",
                from,
                to,
                lines,
                net,
                chart,
                "COP",
                lines.size()
        );
    }

    /**
     * services (servicios).
     *
     * Cuenta cuántas veces se contrató
     * cada servicio adicional.
     */
    private ReportData services(
            LocalDateTime from,
            LocalDateTime to) {

        List<ReportPoint> chart = new ArrayList<>();

        int count = 0;

        // Recorremos las reservas.
        for (Reservation reservation : reservations.findAll()) {

            LocalDateTime paymentDate =
                    reservation.getPaymentReceipt().getPaidAt();

            if (!inside(paymentDate, from, to)) {
                continue;
            }

            // Recorremos los servicios de la reserva.
            for (ServiceOption service : reservation.getServices()) {

                // Sumamos una contratación al servicio.
                add(
                        chart,
                        service.getName(),
                        BigDecimal.ONE
                );

                count++;
            }
        }

        // Ordenamos los servicios más contratados primero.
        sortDescending(chart);

        List<String> lines = new ArrayList<>();

        for (ReportPoint item : chart) {

            lines.add(
                    item.getLabel()
                            + " | Veces contratado: "
                            + item.getValue()
            );
        }

        return result(
                "Servicios contratados (fecha de pago)",
                from,
                to,
                lines,
                BigDecimal.valueOf(count),
                chart,
                "servicios",
                count
        );
    }

    /**
     * recharges (recargas).
     *
     * Calcula el valor de las recargas
     * realizadas en las tarjetas virtuales.
     *
     * No se mezcla con los ingresos del hotel.
     */
    private ReportData recharges(
            LocalDateTime from,
            LocalDateTime to) {

        List<String> lines = new ArrayList<>();
        List<ReportPoint> chart = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;

        // Recorremos los movimientos.
        for (PaymentMovement movement : movements.findAll()) {

            // Ignoramos movimientos no aprobados o fuera del período.
            if (!completedWithin(movement, from, to)) {
                continue;
            }

            // Solo contamos recargas.
            if (movement.getType() != PaymentMovementType.RECHARGE) {
                continue;
            }

            total = total.add(movement.getAmount());

            lines.add(
                    movement.getOccurredAt().toLocalDate()
                            + " | Tarjeta "
                            + movement.getCardId()
                            + " | $"
                            + money(movement.getAmount())
            );

            add(
                    chart,
                    movement.getOccurredAt().toLocalDate().toString(),
                    movement.getAmount()
            );
        }

        return result(
                "Recargas de tarjetas (NO son ingresos)",
                from,
                to,
                lines,
                total,
                chart,
                "COP",
                lines.size()
        );
    }

    /**
     * cancellations (cancelaciones).
     *
     * Consulta las reservas canceladas
     * y los reembolsos realizados.
     */
    private ReportData cancellations(
            LocalDateTime from,
            LocalDateTime to) {

        List<String> lines = new ArrayList<>();
        List<ReportPoint> chart = new ArrayList<>();

        int cancelled = 0;
        int refunded = 0;

        BigDecimal refunds = BigDecimal.ZERO;

        // Consultamos los eventos de reservas.
        for (ReservationEvent event :
                reservationEvents.getActivityHistory()) {

            // Verificamos si es un evento de cancelación.
            if (event.getType() == ReservationEvent.Type.CANCELLED
                    && inside(event.getOccurredAt(), from, to)) {

                cancelled++;

                lines.add(
                        event.getOccurredAt().toLocalDate()
                                + " | Reserva cancelada | "
                                + event.getReservation().getId()
                );
            }
        }

        // Consultamos los movimientos monetarios.
        for (PaymentMovement movement : movements.findAll()) {

            if (!completedWithin(movement, from, to)) {
                continue;
            }

            // Solo consideramos reembolsos.
            if (movement.getType() != PaymentMovementType.REFUND) {
                continue;
            }

            refunded++;

            refunds = refunds.add(movement.getAmount());

            lines.add(
                    movement.getOccurredAt().toLocalDate()
                            + " | Reembolso | $"
                            + money(movement.getAmount())
            );

            add(
                    chart,
                    movement.getOccurredAt().toLocalDate().toString(),
                    movement.getAmount()
            );
        }

        lines.add(
                "RESUMEN: "
                        + cancelled
                        + " cancelaciones; "
                        + refunded
                        + " reembolsos."
        );

        return result(
                "Cancelaciones y reembolsos",
                from,
                to,
                lines,
                refunds,
                chart,
                "COP reembolsados",
                cancelled + refunded
        );
    }

    /**
     * points (puntos).
     *
     * Consulta los movimientos del programa
     * de fidelización.
     *
     * Acumulaciones, redenciones y reversiones.
     */
    private ReportData points(
            LocalDateTime from,
            LocalDateTime to) {

        List<String> lines = new ArrayList<>();
        List<ReportPoint> chart = new ArrayList<>();

        int accrued = 0;
        int redeemed = 0;
        int reverted = 0;

        // Recorremos los clientes.
        for (Customer customer : customers.findAll()) {

            // Recorremos los movimientos de puntos del cliente.
            for (LoyaltyEntry entry :
                    loyalty.findEntries(customer.getId())) {

                if (!inside(entry.at(), from, to)) {
                    continue;
                }

                lines.add(
                        entry.at().toLocalDate()
                                + " | "
                                + customer.getFullName()
                                + " | "
                                + entry.kind()
                                + " | "
                                + entry.points()
                                + " puntos"
                );

                // Clasificamos el movimiento de puntos.
                switch (entry.kind()) {

                    case EARNED:
                        accrued += entry.points();
                        break;

                    case REDEEMED:
                        redeemed += entry.points();
                        break;

                    case REVERTED:
                        reverted += entry.points();
                        break;
                }
            }
        }

        // Datos para la gráfica.
        chart.add(
                new ReportPoint(
                        "Acumulados",
                        BigDecimal.valueOf(accrued)
                )
        );

        chart.add(
                new ReportPoint(
                        "Redimidos",
                        BigDecimal.valueOf(redeemed)
                )
        );

        chart.add(
                new ReportPoint(
                        "Revertidos",
                        BigDecimal.valueOf(reverted)
                )
        );

        lines.add(
                "NOTA: el total es movimiento neto del período; "
                        + "no descuenta vencimientos."
        );

        // Calculamos el movimiento neto de puntos.
        int netPoints = accrued - redeemed - reverted;

        return result(
                "Puntos acumulados y redimidos",
                from,
                to,
                lines,
                BigDecimal.valueOf(netPoints),
                chart,
                "puntos netos",
                lines.size() - 1
        );
    }

    /**
     * result (resultado).
     *
     * Construye el objeto final del informe
     * con sus datos, totales y valores gráficos.
     */
    private ReportData result(
            String title,
            LocalDateTime from,
            LocalDateTime to,
            List<String> rows,
            BigDecimal total,
            List<ReportPoint> points,
            String unit,
            int count) {

        // Mostramos un mensaje cuando no existen resultados.
        if (rows.isEmpty()) {

            rows.add(
                    "No hay registros en el período seleccionado."
            );
        }

        return new ReportData(
                title,
                LocalDateTime.now(),
                from,
                to,
                rows,
                total,
                points,
                unit,
                count
        );
    }

    /**
     * add (acumular).
     *
     * Busca si ya existe una etiqueta.
     *
     * Si existe, suma el valor.
     * Si no existe, agrega un nuevo elemento.
     *
     * No utiliza Map ni HashMap.
     */
    private static void add(
            List<ReportPoint> values,
            String label,
            BigDecimal amount) {

        // Buscamos utilizando un ciclo for.
        for (int i = 0; i < values.size(); i++) {

            if (values.get(i).getLabel().equals(label)) {

                // Sumamos al valor existente.
                BigDecimal newValue =
                        values.get(i).getValue().add(amount);

                // Actualizamos la posición de la lista.
                values.set(
                        i,
                        new ReportPoint(label, newValue)
                );

                return;
            }
        }

        // Si no encontramos la etiqueta, la agregamos.
        values.add(
                new ReportPoint(label, amount)
        );
    }

    /**
     * sortDescending (ordenar descendente).
     *
     * Ordena los datos de mayor a menor
     * mediante dos ciclos for tradicionales.
     */
    private static void sortDescending(List<ReportPoint> values) {

        for (int i = 0; i < values.size(); i++) {

            for (int j = i + 1; j < values.size(); j++) {

                // Comparamos los dos valores.
                if (values.get(j).getValue().compareTo(
                        values.get(i).getValue()) > 0) {

                    // Intercambiamos sus posiciones.
                    ReportPoint temp = values.get(i);

                    values.set(i, values.get(j));

                    values.set(j, temp);
                }
            }
        }
    }

    /**
     * completedWithin (completado dentro del período).
     *
     * Comprueba que el movimiento esté aprobado
     * y que su fecha pertenezca al rango consultado.
     */
    private static boolean completedWithin(
            PaymentMovement movement,
            LocalDateTime from,
            LocalDateTime to) {

        return movement.getStatus() == PaymentMovementStatus.COMPLETED
                && inside(movement.getOccurredAt(), from, to);
    }

    /**
     * inside (dentro del período).
     *
     * Verifica que una fecha esté comprendida
     * entre la fecha inicial y la final.
     */
    private static boolean inside(
            LocalDateTime date,
            LocalDateTime from,
            LocalDateTime to) {

        return date != null
                && !date.isBefore(from)
                && !date.isAfter(to);
    }

    /**
     * money (dinero).
     *
     * Convierte un BigDecimal a texto
     * sin utilizar notación científica.
     */
    private static String money(BigDecimal value) {

        return value.toPlainString();
    }
}