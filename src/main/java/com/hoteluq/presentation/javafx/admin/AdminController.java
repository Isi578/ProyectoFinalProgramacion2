package com.hoteluq.presentation.javafx.admin;

import com.hoteluq.application.admin.AdminDashboardService;
import com.hoteluq.application.reservation.CancellationResult;
import com.hoteluq.application.reporting.DailyFiscalService;
import com.hoteluq.domain.model.ServicePricingUnit;
import com.hoteluq.reporting.HotelReportQuery;
import com.hoteluq.reporting.PdfReportExporter;
import com.hoteluq.reporting.ReportContent;
import com.hoteluq.reporting.SimpleReportContent;
import com.hoteluq.reporting.ReportSummaryDecorator;
import com.hoteluq.reporting.ReportHighlightDecorator;
import com.hoteluq.reporting.ReportData;
import com.hoteluq.reporting.ReportExporter;
import com.hoteluq.reporting.ReportPoint;
import com.hoteluq.reporting.TextReportExporter;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AdminController (controlador del administrador).
 *
 * Conecta los botones de AdminView
 * con los servicios del sistema.
 *
 * Aplica el patrón Decorator cuando el administrador
 * selecciona un informe ampliado.
 *
 * No calcula estadísticas directamente.
 */
public final class AdminController {

    // Referencia a la vista.
    private final AdminView view;

    // Servicio administrativo que implementa Facade.
    private final AdminDashboardService administration;

    // Contrato para consultar los informes.
    private final HotelReportQuery reports;

    // Servicio de informes fiscales.
    private final DailyFiscalService fiscal;

    // Informe que se está mostrando.
    private ReportData currentReport;

    /**
     * Constructor de AdminController.
     *
     * Recibe las clases con las que trabajará
     * el controlador.
     */
    public AdminController(
            AdminView view,
            AdminDashboardService administration,
            HotelReportQuery reports,
            DailyFiscalService fiscal) {

        this.view = view;
        this.administration = administration;
        this.reports = reports;
        this.fiscal = fiscal;
    }

    /**
     * initialize (inicializar).
     *
     * Asigna una acción a cada botón.
     */
    public void initialize() {

        // Botón para ingresar como administrador.
        view.login.setOnAction(event -> login());

        // Botón para actualizar los listados.
        view.refreshOperations.setOnAction(
                event -> execute(() -> refreshOperations())
        );

        // Botón para cancelar una reserva por el hotel.
        view.cancelByHotel.setOnAction(
                event -> execute(() -> {

                    CancellationResult result =
                            administration.cancelByHotel(
                                    adminId(),
                                    view.reservationId.getText().trim()
                            );

                    refreshOperations();

                    view.operationsOutput.appendText(
                            "\nCANCELACIÓN HOTEL: reembolso $"
                                    + result.getRefundAmount().toPlainString()
                                    + " COP\n"
                    );
                })
        );

        // Botón para recargar tarjetas.
        view.rechargeCard.setOnAction(
                event -> execute(() -> {

                    BigDecimal amount =
                            amount(view.rechargeAmount.getText());

                    administration.recharge(
                            adminId(),
                            view.cardId.getText().trim(),
                            amount
                    );

                    refreshOperations();

                    view.operationsOutput.appendText(
                            "\nRECARGA REALIZADA: $"
                                    + amount
                                    + " COP\n"
                    );
                })
        );

        // Botón para agregar servicios.
        view.addService.setOnAction(
                event -> execute(() -> {

                    ServicePricingUnit unit =
                            view.serviceUnit.getValue();

                    administration.addService(
                            adminId(),
                            view.serviceId.getText().trim(),
                            view.serviceName.getText().trim(),
                            amount(view.servicePrice.getText()),
                            unit
                    );

                    refreshOperations();

                    view.operationsOutput.appendText(
                            "\nSERVICIO CREADO\n"
                    );
                })
        );

        // Botón para actualizar servicios.
        view.updateService.setOnAction(
                event -> execute(() -> {

                    administration.updateService(
                            adminId(),
                            view.serviceId.getText().trim(),
                            view.serviceName.getText().trim(),
                            amount(view.servicePrice.getText()),
                            view.serviceUnit.getValue()
                    );

                    refreshOperations();

                    view.operationsOutput.appendText(
                            "\nSERVICIO ACTUALIZADO\n"
                    );
                })
        );

        // Botón para desactivar servicios.
        view.deactivateService.setOnAction(
                event -> execute(() -> {

                    administration.deactivateService(
                            adminId(),
                            view.serviceId.getText().trim()
                    );

                    refreshOperations();

                    view.operationsOutput.appendText(
                            "\nSERVICIO DESACTIVADO\n"
                    );
                })
        );

        // Botón para crear promociones.
        view.createPromotion.setOnAction(
                event -> execute(() -> {

                    // Verificamos las fechas.
                    if (view.promoStart.getValue() == null ||
                            view.promoEnd.getValue() == null) {

                        throw new IllegalArgumentException(
                                "Faltan fechas para la promoción."
                        );
                    }

                    administration.createPromotion(
                            adminId(),
                            view.promoCode.getText().trim(),
                            amount(view.promoPercent.getText()),
                            view.promoStart.getValue().atStartOfDay(),
                            view.promoEnd.getValue().atTime(23, 59, 59),
                            Integer.parseInt(
                                    view.promoMinNights.getText().trim()
                            )
                    );

                    refreshOperations();

                    view.operationsOutput.appendText(
                            "\nPROMOCIÓN CREADA\n"
                    );
                })
        );

        // Botón para activar promociones.
        view.activatePromotion.setOnAction(
                event -> execute(() -> {

                    administration.activatePromotion(
                            adminId(),
                            view.promoCode.getText().trim()
                    );

                    refreshOperations();

                    view.operationsOutput.appendText(
                            "\nPROMOCIÓN ACTIVADA\n"
                    );
                })
        );

        // Botón para desactivar promociones.
        view.deactivatePromotion.setOnAction(
                event -> execute(() -> {

                    administration.deactivatePromotion(
                            adminId(),
                            view.promoCode.getText().trim()
                    );

                    refreshOperations();

                    view.operationsOutput.appendText(
                            "\nPROMOCIÓN DESACTIVADA\n"
                    );
                })
        );

        // Botón para generar informes.
        view.generate.setOnAction(
                event -> execute(this::generate)
        );

        // Selector de informe Simple o Ampliado.
        view.reportStyle.setOnAction(event -> {

            // Si ya existe un informe, lo actualizamos.
            if (currentReport != null) {

                execute(this::generate);
            }
        });

        // Selector de gráficos.
        view.chartKind.setOnAction(event -> {

            if (currentReport != null) {

                drawChart(currentReport);
            }
        });

        // Botón para guardar TXT.
        view.saveTxt.setOnAction(
                event -> execute(
                        () -> save(new TextReportExporter(), "txt")
                )
        );

        // Botón para guardar PDF.
        view.savePdf.setOnAction(
                event -> execute(
                        () -> save(new PdfReportExporter(), "pdf")
                )
        );

        // Botón para consultar el informe fiscal.
        view.seeFiscal.setOnAction(
                event -> execute(() -> {

                    administration.requireAdministrator(adminId());

                    view.fiscalOutput.setText(
                            fiscal.readOnly(view.fiscalDay.getValue())
                    );
                })
        );

        view.reportTitle.setText(
                "Seleccione período y genere el reporte."
        );
    }

    /**
     * login (ingresar).
     *
     * Comprueba el identificador del administrador
     * para habilitar las pestañas.
     */
    private void login() {

        try {

            administration.requireAdministrator(adminId());

            view.tabs.setDisable(false);

            view.sessionInfo.setText(
                    "Modo administrador DEMO (sin autenticación real)"
            );

            view.sessionInfo.setStyle(
                    "-fx-text-fill:#176b57;"
                            + "-fx-font-weight:bold;"
            );

            refreshOperations();

        } catch (RuntimeException error) {

            view.tabs.setDisable(true);

            view.sessionInfo.setText(error.getMessage());

            view.sessionInfo.setStyle(
                    "-fx-text-fill:#a12a2a;"
            );
        }
    }

    /**
     * adminId (identificador del administrador).
     *
     * Obtiene el texto del campo correspondiente.
     */
    private String adminId() {

        return view.administratorId.getText();
    }

    /**
     * refreshOperations (actualizar operaciones).
     *
     * Consulta nuevamente la información administrativa.
     */
    private void refreshOperations() {

        view.operationsOutput.setText(
                administration.listOperations(adminId())
        );
    }

    /**
     * generate (generar).
     *
     * Consulta los datos del informe seleccionado.
     *
     * Después aplica Decorator si el administrador
     * seleccionó la opción "Ampliado".
     */
    private void generate() {

        // Comprobamos el acceso administrativo.
        administration.requireAdministrator(adminId());

        // Validamos que las fechas estén seleccionadas.
        if (view.from.getValue() == null ||
                view.to.getValue() == null) {

            throw new IllegalArgumentException(
                    "Seleccione las dos fechas."
            );
        }

        // Fecha inicial del período.
        LocalDateTime from =
                view.from.getValue().atStartOfDay();

        // Fecha final del período.
        LocalDateTime to =
                view.to.getValue().atTime(
                        23,
                        59,
                        59,
                        999_999_999
                );

        // Consultamos los datos originales del informe.
        ReportData originalReport = reports.generate(
                view.reportType.getValue(),
                from,
                to
        );

        // Creamos el contenido simple.
        ReportContent content =
                new SimpleReportContent(originalReport);

        // Revisamos la opción de presentación.
        if ("Ampliado".equals(view.reportStyle.getValue())) {

            // Primer decorador: resumen.
            content = new ReportSummaryDecorator(
                    content,
                    originalReport
            );

            // Segundo decorador: dato destacado.
            content = new ReportHighlightDecorator(
                    content,
                    originalReport
            );
        }

        // Conservamos totales y gráficos originales.
        currentReport = originalReport.withRows(
                content.getRows()
        );

        // Actualizamos el título y total.
        view.reportTitle.setText(
                currentReport.getTitle()
                        + " | TOTAL: "
                        + currentReport.getTotal().toPlainString()
                        + " "
                        + currentReport.getUnit()
        );

        // Mostramos el informe en la interfaz.
        view.reportOutput.setText(
                TextReportExporter.formatText(currentReport)
        );

        // Dibujamos la gráfica correspondiente.
        drawChart(currentReport);
    }

    /**
     * drawChart (dibujar gráfica).
     *
     * Permite visualizar:
     * - Barras.
     * - Pastel.
     * - Líneas.
     *
     * Utiliza los componentes de JavaFX.
     */
    private void drawChart(ReportData report) {

        String selection = view.chartKind.getValue();

        // Revisamos si existen datos.
        if (report.getPoints().isEmpty()) {

            view.chartPanel.getChildren().setAll(
                    new Label("No hay datos para graficar.")
            );

            return;
        }

        // Gráfica de pastel.
        if ("Pastel".equals(selection)) {

            PieChart pie = new PieChart();

            pie.setTitle(report.getTitle());

            int count = 0;

            // Agregamos los valores positivos.
            for (ReportPoint point : report.getPoints()) {

                if (point.getValue().signum() > 0) {

                    pie.getData().add(
                            new PieChart.Data(
                                    point.getLabel(),
                                    point.getValue().doubleValue()
                            )
                    );

                    count++;
                }
            }

            // Revisamos si pudimos agregar datos.
            if (count == 0) {

                view.chartPanel.getChildren().setAll(
                        new Label(
                                "El gráfico de pastel solo muestra "
                                        + "valores positivos."
                        )
                );

                return;
            }

            pie.setLabelsVisible(true);

            view.chartPanel.getChildren().setAll(pie);

        } else if ("Líneas".equals(selection)) {

            // Gráfica de líneas.
            CategoryAxis x = new CategoryAxis();
            NumberAxis y = new NumberAxis();

            LineChart<String, Number> chart =
                    new LineChart<>(x, y);

            chart.setTitle(report.getTitle());
            chart.setLegendVisible(false);
            chart.setCreateSymbols(true);

            XYChart.Series<String, Number> series =
                    new XYChart.Series<>();

            // Agregamos los datos con un ciclo for.
            for (ReportPoint point : report.getPoints()) {

                series.getData().add(
                        new XYChart.Data<>(
                                point.getLabel(),
                                point.getValue().doubleValue()
                        )
                );
            }

            chart.getData().add(series);

            view.chartPanel.getChildren().setAll(chart);

        } else {

            // Gráfica de barras.
            CategoryAxis x = new CategoryAxis();
            NumberAxis y = new NumberAxis();

            BarChart<String, Number> chart =
                    new BarChart<>(x, y);

            chart.setTitle(report.getTitle());
            chart.setLegendVisible(false);

            XYChart.Series<String, Number> series =
                    new XYChart.Series<>();

            // Agregamos los valores de la gráfica.
            for (ReportPoint point : report.getPoints()) {

                series.getData().add(
                        new XYChart.Data<>(
                                point.getLabel(),
                                point.getValue().doubleValue()
                        )
                );
            }

            chart.getData().add(series);

            view.chartPanel.getChildren().setAll(chart);
        }
    }

    /**
     * save (guardar).
     *
     * Exporta el informe visible en TXT o PDF.
     *
     * Utiliza el patrón Strategy.
     */
    private void save(
            ReportExporter exporter,
            String suffix) throws IOException {

        // Actualizamos los datos antes de exportar.
        generate();

        // Abrimos el selector de archivos.
        FileChooser chooser = new FileChooser();

        chooser.setTitle(
                "Guardar informe " + suffix.toUpperCase()
        );

        // Definimos el nombre sugerido.
        chooser.setInitialFileName(
                "HotelUQ_"
                        + view.reportType.getValue().name()
                        + "."
                        + suffix
        );

        // Definimos el tipo de archivo.
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        suffix.toUpperCase()
                                + " (*."
                                + suffix
                                + ")",
                        "*." + suffix
                )
        );

        // Solicitamos al administrador seleccionar el destino.
        File file = chooser.showSaveDialog(
                view.getRoot().getScene().getWindow()
        );

        // Si cancela la ventana, no guardamos.
        if (file == null) {
            return;
        }

        // Creamos el archivo y escribimos los datos.
        try (FileOutputStream output = new FileOutputStream(file)) {

            exporter.export(currentReport, output);
        }

        view.reportTitle.setText(
                "Informe exportado: " + file.getName()
        );
    }

    /**
     * amount (monto).
     *
     * Convierte el texto ingresado por el usuario
     * a un BigDecimal.
     */
    private static BigDecimal amount(String raw) {

        try {

            return new BigDecimal(raw.trim());

        } catch (RuntimeException problem) {

            throw new IllegalArgumentException(
                    "El valor numérico es inválido."
            );
        }
    }

    /**
     * execute (ejecutar).
     *
     * Ejecuta una acción y controla los errores
     * para evitar que se cierre la aplicación.
     */
    private void execute(CheckedAction action) {

        try {

            action.run();

        } catch (Exception error) {

            String message = error.getMessage();

            if (message == null) {

                message = error.toString();
            }

            view.sessionInfo.setText(
                    "Error: " + message
            );

            view.sessionInfo.setStyle(
                    "-fx-text-fill:#a12a2a;"
            );
        }
    }

    /**
     * CheckedAction (acción que puede producir errores).
     *
     * Permite utilizar un mismo método para ejecutar
     * distintas acciones de los botones.
     */
    @FunctionalInterface
    private interface CheckedAction {

        void run() throws Exception;
    }
}