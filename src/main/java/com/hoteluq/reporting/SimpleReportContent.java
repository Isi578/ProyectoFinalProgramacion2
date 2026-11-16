package com.hoteluq.reporting;

import java.util.ArrayList;
import java.util.List;

/**
 * SimpleReportContent (contenido de informe sencillo).
 *
 * Representa el informe básico.
 *
 * Solo utiliza los datos que ya fueron
 * calculados por el sistema.
 */
public final class SimpleReportContent implements ReportContent {

    // report (informe): contiene los datos originales.
    private final ReportData report;

    /**
     * Constructor de SimpleReportContent.
     *
     * Recibe el informe que vamos a mostrar.
     */
    public SimpleReportContent(ReportData report) {
        this.report = report;
    }

    /**
     * getRows (obtener filas).
     *
     * Devuelve los registros originales.
     * Se realiza una copia para evitar modificaciones.
     */
    @Override
    public List<String> getRows() {

        return new ArrayList<>(report.getRows());
    }
}