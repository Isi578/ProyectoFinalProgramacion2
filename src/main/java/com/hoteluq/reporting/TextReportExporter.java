package com.hoteluq.reporting;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * TextReportExporter (exportador de informes de texto).
 *
 * Implementa Strategy para generar archivos TXT.
 *
 */
public final class TextReportExporter implements ReportExporter {

    // DATE (fecha): establece el formato de presentación.
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * format (formato).
     *
     * Indica que esta implementación genera TXT.
     */
    @Override
    public ReportFormat format() {
        return ReportFormat.TXT;
    }

    /**
     * formatText (dar formato al texto).
     *
     * Construye el informe utilizando StringBuilder.
     *
     * Recorre las filas con un ciclo for.
     */
    public static String formatText(ReportData report) {

        StringBuilder text = new StringBuilder();

        text.append("HOTELUQ | INFORME ADMINISTRATIVO\n");
        text.append("==============================================\n");

        // Título y fechas del informe.
        text.append(report.getTitle()).append('\n');

        text.append("Generado: ")
                .append(DATE.format(report.getGeneratedAt()))
                .append('\n');

        text.append("Desde: ")
                .append(DATE.format(report.getFrom()))
                .append('\n');

        text.append("Hasta: ")
                .append(DATE.format(report.getTo()))
                .append('\n');

        text.append("----------------------------------------------\n");

        // Cantidad real de registros encontrados.
        text.append("REGISTROS (filas): ")
                .append(report.getRecordCount())
                .append('\n');

        // Recorremos todas las filas del informe.
        for (String row : report.getRows()) {

            text.append("  - ")
                    .append(row)
                    .append('\n');
        }

        text.append("----------------------------------------------\n");

        // Total calculado por el sistema.
        text.append("TOTAL: ")
                .append(report.getTotal().toPlainString())
                .append(' ')
                .append(report.getUnit())
                .append('\n');

        return text.toString();
    }

    /**
     * export (exportar).
     *
     * Escribe el contenido en el archivo seleccionado.
     *
     * UTF-8 permite conservar las tildes y las ñ.
     */
    @Override
    public void export(
            ReportData report,
            OutputStream outputStream) throws IOException {

        outputStream.write(
                formatText(report).getBytes(StandardCharsets.UTF_8)
        );

        // flush (vaciar): termina de enviar los datos.
        outputStream.flush();
    }
}