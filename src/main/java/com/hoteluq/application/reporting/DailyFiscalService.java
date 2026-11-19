package com.hoteluq.application.reporting;

import com.hoteluq.application.port.PaymentMovementRepository;
import com.hoteluq.application.port.ReservationRepository;
import com.hoteluq.domain.model.PaymentMovement;
import com.hoteluq.domain.model.PaymentMovementStatus;
import com.hoteluq.domain.model.PaymentMovementType;
import com.hoteluq.domain.model.Reservation;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DailyFiscalService (servicio fiscal diario).
 *
 * Consulta los pagos y reservas del día.
 *
 * Permite generar un informe fiscal diario
 * que no puede modificarse desde la aplicación.
 *
 * Los cierres se guardan como archivos TXT.
 *
 *
 */
public final class DailyFiscalService {

    // Repositorio de reservas.
    private final ReservationRepository reservations;

    // Repositorio de movimientos de tarjetas.
    private final PaymentMovementRepository movements;

    // Carpeta donde guardaremos los archivos fiscales.
    private final Path directory;

    /**
     * Constructor de DailyFiscalService.
     *
     * Recibe los repositorios originales
     * y la ubicación de los archivos.
     */
    public DailyFiscalService(
            ReservationRepository reservations,
            PaymentMovementRepository movements,
            Path directory) {

        this.reservations = reservations;
        this.movements = movements;
        this.directory = directory;
    }

    /**
     * closeYesterdayAutomatically (cerrar ayer automáticamente).
     *
     * Intenta archivar el informe del día anterior.
     *
     * Si ya existe un informe, no lo sobrescribe.
     *
     * Se llama desde la aplicación de forma periódica.
     */
    public synchronized void closeYesterdayAutomatically()
            throws IOException {

        // Obtenemos la fecha de ayer.
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // Construimos la ruta del archivo.
        Path path = fileFor(yesterday);

        // Creamos la carpeta si todavía no existe.
        Files.createDirectories(directory);

        // Solo guardamos cuando aún no existe un archivo.
        if (!Files.exists(path)) {

            String text = calculate(yesterday, true);

            try {

                // CREATE_NEW impide reemplazar un archivo existente.
                Files.writeString(
                        path,
                        text,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                );

            } catch (java.nio.file.FileAlreadyExistsException ignored) {

                // Si el archivo ya existe, conservamos su contenido.
            }
        }
    }

    /**
     * readOnly (leer solamente).
     *
     * Permite consultar el informe fiscal.
     *
     * El día actual se muestra como provisional.
     * Los días cerrados se consultan desde archivo.
     */
    public synchronized String readOnly(LocalDate day)
            throws IOException {

        // Validamos la fecha.
        if (day == null || day.isAfter(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Seleccione una fecha fiscal válida."
            );
        }

        // Buscamos el archivo de la fecha seleccionada.
        Path path = fileFor(day);

        // Si existe, devolvemos su contenido.
        if (Files.exists(path)) {

            return Files.readString(
                    path,
                    StandardCharsets.UTF_8
            );
        }

        // El día actual todavía no está cerrado.
        if (day.equals(LocalDate.now())) {

            return calculate(day, false);
        }

        // No inventamos datos históricos inexistentes.
        throw new IOException(
                "No hay cierre fiscal archivado para "
                        + day
                        + ". Los datos de la aplicación son temporales."
        );
    }

    /**
     * fileFor (archivo para la fecha).
     *
     * Construye el nombre del archivo fiscal.
     */
    /**
     * Cierra expresamente días anteriores para la demostración opcional.
     * Jamás sobrescribe archivos archivados y no se ejecuta al iniciar.
     */
    public synchronized int archiveDemonstrationDays(List<LocalDate> days) throws IOException {
        int created = 0;
        Files.createDirectories(directory);
        for (LocalDate day : days.stream().distinct().sorted().toList()) {
            if (day == null || !day.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Solo se cierran días anteriores al actual.");
            }
            try {
                Files.writeString(fileFor(day), calculate(day, true),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                created++;
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
                // Un cierre existente no debe editarse, ni siquiera en modo demo.
            }
        }
        return created;
    }

    private Path fileFor(LocalDate day) {

        return directory.resolve(
                "fiscal-" + day + ".txt"
        );
    }

    /**
     * calculate (calcular).
     *
     * Genera el contenido del informe fiscal.
     *
     * Utiliza ciclos for y condicionales if.
     */
    private String calculate(
            LocalDate day,
            boolean closed) {

        // Cantidad de reservas pagadas.
        int reservationsCount = 0;

        // Guardamos habitaciones distintas sin mostrarlas.
        List<String> roomNumbers = new ArrayList<>();

        // Recorremos las reservas del hotel.
        for (Reservation reservation : reservations.findAll()) {

            LocalDate paymentDate =
                    reservation.getPaymentReceipt()
                            .getPaidAt()
                            .toLocalDate();

            // Solo contamos reservas pagadas en esta fecha.
            if (!paymentDate.equals(day)) {
                continue;
            }

            reservationsCount++;

            String number = reservation.getRoom().getNumber();

            // Evitamos contar dos veces una misma habitación.
            if (!roomNumbers.contains(number)) {

                roomNumbers.add(number);
            }
        }

        // Cantidad de pagos aprobados.
        int paymentsCount = 0;

        // Totales monetarios.
        BigDecimal charges = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        BigDecimal recharges = BigDecimal.ZERO;

        // Recorremos los movimientos de las tarjetas.
        for (PaymentMovement movement : movements.findAll()) {

            // Solo contamos movimientos completados.
            if (movement.getStatus() != PaymentMovementStatus.COMPLETED) {
                continue;
            }

            // Solo movimientos de la fecha consultada.
            if (!movement.getOccurredAt().toLocalDate().equals(day)) {
                continue;
            }

            // Clasificamos el movimiento.
            if (movement.getType() == PaymentMovementType.CHARGE) {

                paymentsCount++;

                // add (sumar): agrega el monto del cobro.
                charges = charges.add(movement.getAmount());

            } else if (movement.getType() == PaymentMovementType.REFUND) {

                // Sumamos las devoluciones.
                refunds = refunds.add(movement.getAmount());

            } else if (movement.getType() == PaymentMovementType.RECHARGE) {

                // Recargas: no son ingresos del hotel.
                recharges = recharges.add(movement.getAmount());
            }
        }

        // Construimos el informe fiscal.
        StringBuilder text = new StringBuilder();

        text.append("HOTELUQ - INFORME FISCAL DIARIO\n");
        text.append("==================================\n");

        text.append("Fecha fiscal: ")
                .append(day)
                .append('\n');

        // Indicamos si el día está cerrado.
        if (closed) {

            text.append("Estado: CERRADO - SOLO LECTURA\n");

        } else {

            text.append("Estado: PROVISIONAL - SOLO LECTURA\n");
        }

        text.append("Reservas pagadas: ")
                .append(reservationsCount)
                .append('\n');

        text.append("Habitaciones diferentes reservadas: ")
                .append(roomNumbers.size())
                .append('\n');

        text.append("Pagos aprobados: ")
                .append(paymentsCount)
                .append('\n');

        text.append("Total de pagos COP: ")
                .append(charges.toPlainString())
                .append('\n');

        text.append("Reembolsos COP: ")
                .append(refunds.toPlainString())
                .append('\n');

        // subtract (restar): calcula ingresos menos reembolsos.
        BigDecimal netRevenue = charges.subtract(refunds);

        text.append("Ingreso neto COP: ")
                .append(netRevenue.toPlainString())
                .append('\n');

        text.append("Recargas de tarjetas COP (no son ingresos): ")
                .append(recharges.toPlainString())
                .append('\n');

        text.append(
                "Este informe no identifica habitaciones ni clientes.\n"
        );

        return text.toString();
    }
}