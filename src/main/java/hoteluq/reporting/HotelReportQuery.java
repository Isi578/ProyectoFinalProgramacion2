package hoteluq.reporting;

import java.time.LocalDateTime;

/**
 * HotelReportQuery (consulta de informes del hotel).
 *
 * Define el contrato para consultar reportes.
 *
 * La interfaz gráfica no necesita conocer
 * cómo se realizan internamente los cálculos.
 */
public interface HotelReportQuery {

    /**
     * generate (generar).
     *
     * Recibe:
     * type: tipo de informe.
     * from: fecha inicial.
     * to: fecha final.
     *
     * Devuelve los datos correspondientes.
     */
    ReportData generate(
            ReportType type,
            LocalDateTime from,
            LocalDateTime to
    );
}