package hoteluq.reporting;

import java.math.BigDecimal;

/**
 * ReportPoint (punto del informe).
 *
 * Representa un dato que se utilizará
 * para construir una gráfica.
 */
public final class ReportPoint {

    // label (etiqueta): nombre del dato.
    private final String label;

    // value (valor): cantidad correspondiente.
    private final BigDecimal value;

    public ReportPoint(String label, BigDecimal value) {
        this.label = label;
        this.value = value;
    }

    // getLabel (obtener etiqueta).
    public String getLabel() {
        return label;
    }

    // getValue (obtener valor).
    public BigDecimal getValue() {
        return value;
    }
}