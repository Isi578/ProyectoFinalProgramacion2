package com.hoteluq.reporting;

import java.util.List;

/**
 * ReportDecorator (decorador de informes).
 *
 * Clase base del patrón Decorator.
 *
 * Su función es guardar una referencia
 * a otro informe.
 *
 * Las clases que hereden de ReportDecorator
 * podrán agregar información sin modificar
 * el informe original.
 */
public abstract class ReportDecorator implements ReportContent {

    // original (original): informe que estamos decorando.
    protected final ReportContent original;

    /**
     * Constructor de ReportDecorator.
     *
     * Recibe el contenido que queremos ampliar.
     */
    protected ReportDecorator(ReportContent original) {

        if (original == null) {

            throw new IllegalArgumentException(
                    "Se necesita un contenido original."
            );
        }

        this.original = original;
    }

    /**
     * getRows (obtener filas).
     *
     * Solicita las filas del informe original.
     *
     * Los decoradores que hereden de esta clase
     * podrán agregar más información.
     */
    @Override
    public List<String> getRows() {

        return original.getRows();
    }
}