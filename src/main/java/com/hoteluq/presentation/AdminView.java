package com.hoteluq.presentation;

import com.hoteluq.domain.model.ServicePricingUnit;
import com.hoteluq.reporting.ReportType;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * AdminView (vista del administrador).
 *
 * Interfaz en JavaFX.
 *
 * No realiza cálculos ni modifica reservas.
 *
 * La lógica se encuentra
 * en AdminController.
 */
public final class AdminView {

    // root (raíz): contenedor principal de la vista.
    private final BorderPane root = new BorderPane();

    // Campos de acceso administrativo.
    final TextField administratorId = new TextField();
    final Button login = new Button("Ingresar");

    final Label sessionInfo =
            new Label("Acceso administrativo de demostración");

    final TabPane tabs = new TabPane();

    // Elementos para consultar operaciones.
    final TextArea operationsOutput = outputArea();

    final Button refreshOperations =
            new Button("Actualizar listados");

    // Elementos para cancelar reservas.
    final TextField reservationId =
            field("ID de reserva", 250);

    final Button cancelByHotel =
            new Button("Cancelar por el hotel");

    // Elementos para recargar tarjetas.
    final TextField cardId =
            field("ID de tarjeta", 170);

    final TextField rechargeAmount =
            field("Monto COP", 130);

    final Button rechargeCard =
            new Button("Recargar tarjeta");

    // Elementos para gestionar servicios.
    final TextField serviceId =
            field("ID servicio", 120);

    final TextField serviceName =
            field("Nombre servicio", 150);

    final TextField servicePrice =
            field("Valor COP", 115);

    final ComboBox<ServicePricingUnit> serviceUnit =
            new ComboBox<>();

    final Button addService =
            new Button("Agregar servicio");

    final Button updateService =
            new Button("Actualizar servicio");

    final Button deactivateService =
            new Button("Desactivar servicio");

    // Elementos para gestionar promociones.
    final TextField promoCode =
            field("Código de promoción", 150);

    final TextField promoPercent =
            field("Descuento %", 105);

    final TextField promoMinNights =
            field("Mín. noches", 100);

    final DatePicker promoStart =
            new DatePicker(LocalDate.now());

    final DatePicker promoEnd =
            new DatePicker(LocalDate.now().plusMonths(2));

    final Button createPromotion =
            new Button("Crear promoción");

    final Button deactivatePromotion =
            new Button("Desactivar promoción");

    final Button activatePromotion =
            new Button("Activar promoción");

    // Elementos para generar reportes.
    final ComboBox<ReportType> reportType =
            new ComboBox<>();

    final DatePicker from =
            new DatePicker(LocalDate.now().minusDays(30));

    final DatePicker to =
            new DatePicker(LocalDate.now());

    final Button generate =
            new Button("Generar informe");

    final Button saveTxt =
            new Button("Exportar TXT");

    final Button savePdf =
            new Button("Exportar PDF");

    // Selector del tipo de gráfica.
    final ComboBox<String> chartKind =
            new ComboBox<>();

    /**
     * reportStyle (estilo de informe).
     *
     * Permite elegir entre:
     * - Simple.
     * - Ampliado.
     *
     * Esta opción se utiliza para aplicar Decorator.
     */
    final ComboBox<String> reportStyle =
            new ComboBox<>();

    final Label reportTitle =
            new Label("Seleccione el informe y las fechas.");

    final TextArea reportOutput =
            outputArea();

    // Espacio donde se mostrarán las gráficas.
    final StackPane chartPanel =
            new StackPane();

    // Elementos del informe fiscal.
    final DatePicker fiscalDay =
            new DatePicker(LocalDate.now());

    final Button seeFiscal =
            new Button("Consultar informe fiscal");

    final TextArea fiscalOutput =
            outputArea();

    /**
     * Constructor de AdminView.
     *
     * Organiza la ventana administrativa
     * y sus tres pestañas principales.
     */
    public AdminView() {

        Label heading = new Label(
                "HotelUQ | Panel de administración"
        );

        heading.setStyle(
                "-fx-font-size:22px;"
                        + "-fx-font-weight:bold;"
                        + "-fx-text-fill:#173f35;"
        );

        administratorId.setPromptText(
                "ID del administrador"
        );

        administratorId.setPrefWidth(210);

        login.setStyle(
                "-fx-background-color:#176b57;"
                        + "-fx-text-fill:white;"
                        + "-fx-font-weight:bold;"
        );

        // Encabezado de la ventana.
        VBox header = new VBox(
                9,
                heading,
                new HBox(
                        12,
                        new Label("Administrador:"),
                        administratorId,
                        login,
                        sessionInfo
                )
        );

        header.setPadding(new Insets(15));

        header.setStyle(
                "-fx-background-color:#ecf5f2;"
        );

        // Creamos las tres pestañas.
        Tab operations = new Tab(
                "Operaciones",
                makeOperations()
        );

        Tab reports = new Tab(
                "Estadísticas y reportes",
                makeReports()
        );

        Tab fiscal = new Tab(
                "Fiscal diario",
                makeFiscal()
        );

        // Impedimos cerrar las pestañas.
        operations.setClosable(false);
        reports.setClosable(false);
        fiscal.setClosable(false);

        tabs.getTabs().addAll(
                operations,
                reports,
                fiscal
        );

        // Las pestañas se habilitan al ingresar.
        tabs.setDisable(true);

        root.setTop(header);
        root.setCenter(tabs);
    }

    /**
     * makeOperations (crear panel de operaciones).
     *
     * Organiza los campos de reservas, tarjetas,
     * servicios y promociones.
     */
    private Parent makeOperations() {

        Label note = label(
                "Las operaciones utilizan los servicios "
                        + "de los otros módulos."
        );

        HBox row1 = row(
                new Label("Reservas:"),
                reservationId,
                cancelByHotel
        );

        HBox row2 = row(
                new Label("Tarjetas:"),
                cardId,
                rechargeAmount,
                rechargeCard
        );

        // Seleccionamos un mínimo de una noche.
        promoMinNights.setText("1");

        // Cargamos las unidades disponibles de servicios.
        serviceUnit.getItems().addAll(
                ServicePricingUnit.values()
        );

        serviceUnit.getSelectionModel().selectFirst();

        HBox row3 = row(
                new Label("Servicios:"),
                serviceId,
                serviceName,
                servicePrice,
                serviceUnit,
                addService,
                updateService,
                deactivateService
        );

        HBox row4 = row(
                new Label("Promociones:"),
                promoCode,
                promoPercent,
                promoMinNights,
                promoStart,
                promoEnd,
                createPromotion,
                activatePromotion,
                deactivatePromotion
        );

        operationsOutput.setPrefRowCount(17);

        VBox area = new VBox(
                12,
                note,
                row1,
                row2,
                row3,
                row4,
                refreshOperations,
                operationsOutput
        );

        area.setPadding(new Insets(16));

        // Permite desplazarse si el contenido es grande.
        ScrollPane scroll = new ScrollPane(area);

        scroll.setFitToWidth(true);

        return scroll;
    }

    /**
     * makeReports (crear panel de reportes).
     *
     * Crea los controles para seleccionar:
     * - Tipo de informe.
     * - Fechas.
     * - Tipo de gráfica.
     * - Presentación simple o ampliada.
     * - Exportación TXT o PDF.
     */
    private Parent makeReports() {

        // Cargamos los ocho informes disponibles.
        reportType.getItems().addAll(
                ReportType.values()
        );

        reportType.setPrefWidth(245);

        // Mostramos los nombres de los reportes en español.
        reportType.setConverter(
                new javafx.util.StringConverter<ReportType>() {

                    @Override
                    public String toString(ReportType type) {

                        if (type == null) {
                            return "";
                        }

                        switch (type) {

                            case RESERVATIONS_BY_PERIOD:
                                return "Reservas por período";

                            case MOST_BOOKED_ROOMS:
                                return "Habitaciones más reservadas";

                            case OCCUPANCY:
                                return "Ocupación";

                            case REVENUE_BY_PERIOD:
                                return "Ingresos";

                            case ADDITIONAL_SERVICES:
                                return "Servicios contratados";

                            case RECHARGES:
                                return "Recargas";

                            case CANCELLATIONS_AND_REFUNDS:
                                return "Cancelaciones y reembolsos";

                            case CUSTOMER_POINTS:
                                return "Puntos acumulados / redimidos";

                            default:
                                return "";
                        }
                    }

                    @Override
                    public ReportType fromString(String value) {
                        return null;
                    }
                }
        );

        // Seleccionamos el primer informe.
        reportType.getSelectionModel().selectFirst();

        // Opciones gráficas.
        chartKind.getItems().addAll(
                "Barras",
                "Pastel",
                "Líneas"
        );

        chartKind.getSelectionModel().selectFirst();

        // Opciones del patrón Decorator.
        reportStyle.getItems().addAll(
                "Simple",
                "Ampliado"
        );

        reportStyle.getSelectionModel().selectFirst();

        reportTitle.setStyle(
                "-fx-font-size:15px;"
                        + "-fx-font-weight:bold;"
                        + "-fx-text-fill:#173f35;"
        );

        // Filtros del reporte.
        HBox filters = row(
                reportType,
                new Label("Desde:"),
                from,
                new Label("Hasta:"),
                to,
                generate
        );

        // Opciones de visualización y exportación.
        HBox actions = row(
                new Label("Gráfico:"),
                chartKind,
                new Label("Presentación:"),
                reportStyle,
                saveTxt,
                savePdf
        );

        chartPanel.setMinHeight(230);

        chartPanel.setStyle(
                "-fx-border-color:#d3dfda;"
                        + "-fx-background-color:white;"
        );

        reportOutput.setPrefRowCount(17);

        // Dividimos la pantalla entre texto y gráfica.
        SplitPane result = new SplitPane(
                reportOutput,
                chartPanel
        );

        result.setDividerPositions(0.47);

        VBox body = new VBox(
                11,
                filters,
                actions,
                reportTitle,
                result
        );

        body.setPadding(new Insets(15));

        VBox.setVgrow(
                result,
                javafx.scene.layout.Priority.ALWAYS
        );

        return body;
    }

    /**
     * makeFiscal (crear panel fiscal).
     *
     * Permite consultar informes fiscales.
     *
     * El contenido no puede editarse
     * desde la interfaz.
     */
    private Parent makeFiscal() {

        fiscalOutput.setEditable(false);

        fiscalOutput.setPrefRowCount(18);

        VBox body = new VBox(
                12,
                label(
                        "El cierre del día anterior se archiva "
                                + "automáticamente y no admite edición."
                ),
                row(
                        new Label("Fecha:"),
                        fiscalDay,
                        seeFiscal
                ),
                fiscalOutput
        );

        body.setPadding(new Insets(18));

        VBox.setVgrow(
                fiscalOutput,
                javafx.scene.layout.Priority.ALWAYS
        );

        return body;
    }

    /**
     * getRoot (obtener raíz).
     *
     * Devuelve el contenedor principal de la vista.
     */
    public Parent getRoot() {
        return root;
    }

    /**
     * field (crear campo).
     *
     * Crea un campo de texto con un mensaje
     * y un ancho determinado.
     */
    private static TextField field(
            String placeholder,
            int width) {

        TextField field = new TextField();

        field.setPromptText(placeholder);
        field.setPrefWidth(width);

        return field;
    }

    /**
     * outputArea (crear área de resultados).
     *
     * Crea un área de texto de solo lectura.
     */
    private static TextArea outputArea() {

        TextArea area = new TextArea();

        area.setEditable(false);
        area.setWrapText(true);

        area.setStyle(
                "-fx-font-family:monospace;"
                        + "-fx-font-size:12px;"
        );

        return area;
    }

    /**
     * label (crear etiqueta).
     *
     * Crea una etiqueta con formato.
     */
    private static Label label(String message) {

        Label label = new Label(message);

        label.setStyle(
                "-fx-text-fill:#335f55;"
                        + "-fx-font-size:13px;"
        );

        return label;
    }

    /**
     * row (crear fila).
     *
     * Agrupa los controles horizontalmente.
     */
    private static HBox row(javafx.scene.Node... nodes) {

        HBox result = new HBox(8, nodes);

        result.setAlignment(
                javafx.geometry.Pos.CENTER_LEFT
        );

        return result;
    }
}