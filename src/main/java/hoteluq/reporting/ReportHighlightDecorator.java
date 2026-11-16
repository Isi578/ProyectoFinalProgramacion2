package hoteluq.reporting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportHighlightDecorator (decorador de dato destacado).
 *
 * Busca el dato de mayor magnitud
 * dentro de los valores de la gráfica.
 *
 * Utiliza un ciclo for tradicional.
 *
 * No modifica el informe original.
 */
public final class ReportHighlightDecorator extends ReportDecorator {

    // report (informe): contiene los datos para la gráfica.
    private final ReportData report;

    /**
     * Constructor de ReportHighlightDecorator.
     *
     * original: informe que vamos a decorar.
     * report: información calculada por el sistema.
     */
    public ReportHighlightDecorator(
            ReportContent original,
            ReportData report) {

        super(original);

        this.report = report;
    }

    /**
     * getRows (obtener filas).
     *
     * Busca el valor de mayor magnitud
     * y agrega una observación al informe.
     */
    @Override
    public List<String> getRows() {

        // Copiamos las filas del contenido anterior.
        List<String> rows = new ArrayList<>(original.getRows());

        rows.add("=== DATO DESTACADO ===");

        // largest (mayor): guardará el punto destacado.
        ReportPoint largest = null;

        // Recorremos todos los puntos de la gráfica.
        for (ReportPoint point : report.getPoints()) {

            // Si todavía no tenemos un punto,
            // guardamos el primero.
            if (largest == null) {

                largest = point;

            } else {

                // Comparamos la magnitud de ambos valores.
                if (point.getValue().abs().compareTo(
                        largest.getValue().abs()) > 0) {

                    largest = point;
                }
            }
        }

        // Revisamos si encontramos información.
        if (largest == null) {

            rows.add(
                    "No hay valores para comparar en este periodo."
            );

        } else {

            // amount (monto): valor del dato destacado.
            BigDecimal amount = largest.getValue();

            rows.add(
                    "Mayor magnitud en el gráfico: "
                            + largest.getLabel()
                            + " = "
                            + amount.toPlainString()
            );

            rows.add(
                    "Nota: se compara el valor absoluto, "
                            + "incluso si es negativo."
            );
        }

        return rows;
    }
}