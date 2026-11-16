package com.hoteluq.reporting;

import java.util.List;

/**
 * ReportContent (contenido del informe).
 *
 * Contrato común del patrón Decorator.
 *
 * Todas las clases que implementen esta interfaz
 * deben poder entregar las filas del informe.
 */
public interface ReportContent {

    /**
     * getRows (obtener filas).
     *
     * Devuelve las líneas de texto que
     * se mostrarán en el informe.
     */
    List<String> getRows();
}