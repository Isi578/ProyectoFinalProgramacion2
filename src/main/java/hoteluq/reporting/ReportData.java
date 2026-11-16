package hoteluq.reporting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportData (datos del informe).
 *
 * Guarda los resultados obtenidos del sistema.
 * No calcula reservas, pagos ni estadísticas.
 */
public final class ReportData {

    private final String title;
    private final LocalDateTime generatedAt;
    private final LocalDateTime from;
    private final LocalDateTime to;
    private final List<String> rows;
    private final BigDecimal total;
    private final List<ReportPoint> points;
    private final String unit;
    private final int recordCount;

    /**
     * Constructor para informes sencillos.
     */
    public ReportData(
            String title,
            LocalDateTime generatedAt,
            LocalDateTime from,
            LocalDateTime to,
            List<String> rows,
            BigDecimal total) {

        this(
                title,
                generatedAt,
                from,
                to,
                rows,
                total,
                new ArrayList<ReportPoint>(),
                "COP",
                rows.size()
        );
    }

    /**
     * Constructor para informes con datos gráficos.
     */
    public ReportData(
            String title,
            LocalDateTime generatedAt,
            LocalDateTime from,
            LocalDateTime to,
            List<String> rows,
            BigDecimal total,
            List<ReportPoint> points,
            String unit) {

        this(
                title,
                generatedAt,
                from,
                to,
                rows,
                total,
                points,
                unit,
                rows.size()
        );
    }

    /**
     * Constructor completo de ReportData.
     *
     * Permite indicar por separado la cantidad
     * de registros reales del informe.
     */
    public ReportData(
            String title,
            LocalDateTime generatedAt,
            LocalDateTime from,
            LocalDateTime to,
            List<String> rows,
            BigDecimal total,
            List<ReportPoint> points,
            String unit,
            int recordCount) {

        this.title = title;
        this.generatedAt = generatedAt;
        this.from = from;
        this.to = to;

        // Copiamos los registros originales.
        this.rows = new ArrayList<>(rows);

        this.total = total;

        // Copiamos los datos para las gráficas.
        this.points = new ArrayList<>(points);

        this.unit = unit;
        this.recordCount = recordCount;
    }

    // getTitle (obtener título).
    public String getTitle() {
        return title;
    }

    // getGeneratedAt (obtener fecha de generación).
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    // getFrom (obtener fecha inicial).
    public LocalDateTime getFrom() {
        return from;
    }

    // getTo (obtener fecha final).
    public LocalDateTime getTo() {
        return to;
    }

    // getRows (obtener filas).
    public List<String> getRows() {
        return new ArrayList<>(rows);
    }

    // getTotal (obtener total).
    public BigDecimal getTotal() {
        return total;
    }

    // getPoints (obtener puntos de la gráfica).
    public List<ReportPoint> getPoints() {
        return new ArrayList<>(points);
    }

    // getUnit (obtener unidad).
    public String getUnit() {
        return unit;
    }

    // getRecordCount (obtener cantidad de registros).
    public int getRecordCount() {
        return recordCount;
    }

    /**
     * withRows (con filas).
     *
     * Crea una copia del informe con nuevas filas.
     *
     * Se utiliza después de aplicar Decorator.
     *
     * No modifica el total, los datos gráficos
     * ni la cantidad real de registros.
     */
    public ReportData withRows(List<String> newRows) {

        return new ReportData(
                title,
                generatedAt,
                from,
                to,
                newRows,
                total,
                points,
                unit,
                recordCount
        );
    }
}