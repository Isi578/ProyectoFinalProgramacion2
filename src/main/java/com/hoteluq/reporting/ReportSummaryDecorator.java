package com.hoteluq.reporting;

import java.util.ArrayList;
import java.util.List;

/**
 * ReportSummaryDecorator (decorador de resumen).
 *
 * Agrega un resumen general al informe.
 *
 * No modifica los cálculos originales.
 * Simplemente agrega información al principio.
 */
public final class ReportSummaryDecorator extends ReportDecorator {

    // report (informe): datos necesarios para el resumen.
    private final ReportData report;

    /**
     * Constructor de ReportSummaryDecorator.
     *
     * original: contenido que vamos a decorar.
     * report: datos originales del informe.
     */
    public ReportSummaryDecorator(
            ReportContent original,
            ReportData report) {

        super(original);

        this.report = report;
    }

    /**
     * getRows (obtener filas).
     *
     * Construye una lista con:
     * 1. Resumen general.
     * 2. Registros originales del informe.
     */
    @Override
    public List<String> getRows() {

        // Lista donde guardaremos el contenido ampliado.
        List<String> rows = new ArrayList<>();

        // Agregamos el encabezado del resumen.
        rows.add("=== RESUMEN GENERAL ===");

        // Mostramos la cantidad real de registros.
        rows.add(
                "Registros encontrados: "
                        + report.getRecordCount()
        );

        // Mostramos el total que ya calculó el sistema.
        rows.add(
                "Total del informe: "
                        + report.getTotal().toPlainString()
                        + " "
                        + report.getUnit()
        );

        // Mostramos cuántas categorías tiene la gráfica.
        rows.add(
                "Categorías para la gráfica: "
                        + report.getPoints().size()
        );

        // Separamos el resumen de los registros.
        rows.add("=== DETALLE ===");

        // Conservamos todas las filas del informe original.
        rows.addAll(original.getRows());

        return rows;
    }
}