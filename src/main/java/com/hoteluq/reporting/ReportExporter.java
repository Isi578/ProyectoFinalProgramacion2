package com.hoteluq.reporting;

import java.io.IOException;
import java.io.OutputStream;

/**
 * ReportExporter (exportador de informes).
 *
 * Interfaz correspondiente al patrón Strategy.
 *
 * Permite utilizar diferentes formatos
 * de exportación mediante un mismo contrato.
 */
public interface ReportExporter {

    /**
     * format (formato).
     *
     * Indica qué tipo de archivo genera
     * la implementación.
     */
    ReportFormat format();

    /**
     * export (exportar).
     *
     * Recibe el informe y el destino
     * donde se guardará.
     */
    void export(
            ReportData report,
            OutputStream outputStream
    ) throws IOException;
}