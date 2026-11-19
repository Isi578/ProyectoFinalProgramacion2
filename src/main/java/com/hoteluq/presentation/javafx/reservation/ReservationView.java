package com.hoteluq.presentation.javafx.reservation;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * ReservationView (vista de reservas).
 *
 * Construye los elementos JavaFX del formulario
 * de reservas del cliente.
 *
 * No valida datos ni llama a los servicios.
 *
 * La lógica de los botones se encuentra
 * en ReservationController.
 */
public final class ReservationView {

    // Estilos compartidos con las demás vistas.
    private static final String CARD_STYLE =
            "-fx-background-color:white;"
                    + "-fx-border-color:#d3dfda;";

    private static final String PRIMARY_BUTTON_STYLE =
            "-fx-background-color:#176b57;"
                    + "-fx-text-fill:white;"
                    + "-fx-font-weight:bold;";

    private static final String DANGER_BUTTON_STYLE =
            "-fx-background-color:#a12a2a;"
                    + "-fx-text-fill:white;"
                    + "-fx-font-weight:bold;";

    private static final String SECONDARY_BUTTON_STYLE =
            "-fx-background-color:white;"
                    + "-fx-border-color:#176b57;"
                    + "-fx-text-fill:#176b57;"
                    + "-fx-font-weight:bold;";

    // root (raíz): contenedor principal de la vista.
    private final BorderPane root = new BorderPane();

    private final DatePicker checkInDate =
            new DatePicker(LocalDate.now().plusDays(7));

    private final DatePicker checkOutDate =
            new DatePicker(LocalDate.now().plusDays(8));

    private final TextField checkInTime = new TextField("15:00");

    private final TextField checkOutTime = new TextField("11:00");

    // El ID del cliente lo asigna el inicio de sesión.
    private final TextField customerId = new TextField();

    private final TextField roomNumber = new TextField();

    private final TextField cardId = new TextField();

    private final TextField serviceIds = new TextField();

    private final TextField promotionCode = new TextField();

    private final TextField reservationId = new TextField();

    private final Spinner<Integer> guestCount =
            new Spinner<>(1, 6, 1);

    private final TextArea output = new TextArea();

    // Resumen de la sesión iniciada.
    private final Label sessionLabel = new Label();

    private final Button availabilityButton =
            styledButton("Consultar disponibilidad",
                    PRIMARY_BUTTON_STYLE);

    private final Button createButton =
            styledButton("Crear reserva",
                    PRIMARY_BUTTON_STYLE);

    private final Button cancelButton =
            styledButton("Cancelar reserva",
                    DANGER_BUTTON_STYLE);

    private final Button historyButton =
            styledButton("Actualizar historial",
                    SECONDARY_BUTTON_STYLE);

    /**
     * Constructor de ReservationView.
     *
     * Organiza el encabezado, el formulario
     * y el área de resultados.
     */
    public ReservationView() {

        Label title = new Label("HotelUQ | Reservas");

        title.setStyle(
                "-fx-font-size:22px;"
                        + "-fx-font-weight:bold;"
                        + "-fx-text-fill:#173f35;"
        );

        sessionLabel.setStyle(
                "-fx-text-fill:#335f55;"
                        + "-fx-font-size:13px;"
        );

        VBox header = new VBox(4, title, sessionLabel);

        header.setPadding(new Insets(0, 0, 12, 0));

        checkInTime.setPromptText("HH:mm");
        checkOutTime.setPromptText("HH:mm");
        roomNumber.setPromptText("Ej.: 101");
        cardId.setPromptText("Ej.: CARD-ANA-001");
        serviceIds.setPromptText("Ej.: BREAKFAST, SPA");
        promotionCode.setPromptText("Opcional");
        reservationId.setPromptText("Se completa al crear la reserva");

        customerId.setEditable(false);

        GridPane form = new GridPane();

        form.setHgap(10);
        form.setVgap(10);

        int row = 0;

        form.addRow(row++,
                label("Fecha de entrada"), checkInDate,
                label("Hora de entrada"), checkInTime);

        form.addRow(row++,
                label("Fecha de salida"), checkOutDate,
                label("Hora de salida"), checkOutTime);

        form.addRow(row++,
                label("ID de cliente"), customerId,
                label("Huéspedes"), guestCount);

        form.addRow(row++,
                label("Número de habitación"), roomNumber,
                label("ID de tarjeta"), cardId);

        form.addRow(row++,
                label("Servicios (separados por coma)"), serviceIds,
                label("Código de promoción"), promotionCode);

        form.addRow(row++,
                label("ID de reserva"), reservationId);

        HBox actions = new HBox(
                8,
                availabilityButton,
                createButton,
                cancelButton,
                historyButton
        );

        VBox formCard = new VBox(14, form, actions);

        formCard.setPadding(new Insets(16));

        formCard.setStyle(CARD_STYLE);

        output.setEditable(false);

        output.setWrapText(true);

        output.setPrefRowCount(14);

        VBox outputCard = new VBox(
                8,
                sectionTitle("Resultados"),
                output
        );

        outputCard.setPadding(new Insets(16));

        outputCard.setStyle(CARD_STYLE);

        VBox content = new VBox(12, formCard, outputCard);

        root.setTop(header);

        root.setCenter(content);

        root.setPadding(new Insets(16));

        root.setStyle("-fx-background-color:#ecf5f2;");

        clearSession();
    }

    /**
     * showSession (mostrar sesión).
     *
     * Muestra el cliente que inició sesión y bloquea
     * el campo del ID para que no se pueda cambiar.
     */
    public void showSession(String fullName, String id) {

        sessionLabel.setText(
                "Cliente: " + fullName + " (" + id + ")"
        );

        customerId.setText(id);
    }

    /**
     * clearSession (limpiar sesión).
     *
     * Deja el formulario sin datos del cliente anterior.
     */
    public void clearSession() {

        sessionLabel.setText("Sin sesión iniciada");

        customerId.clear();

        cardId.clear();

        reservationId.clear();

        roomNumber.clear();

        serviceIds.clear();

        promotionCode.clear();
    }

    /**
     * setActionsDisabled (activar o desactivar acciones).
     */
    public void setActionsDisabled(boolean disabled) {

        availabilityButton.setDisable(disabled);

        createButton.setDisable(disabled);

        cancelButton.setDisable(disabled);

        historyButton.setDisable(disabled);
    }

    private static Label label(String text) {

        Label label = new Label(text);

        label.setStyle(
                "-fx-text-fill:#335f55;"
                        + "-fx-font-size:13px;"
        );

        return label;
    }

    private static Label sectionTitle(String text) {

        Label title = new Label(text);

        title.setStyle(
                "-fx-font-size:15px;"
                        + "-fx-font-weight:bold;"
                        + "-fx-text-fill:#173f35;"
        );

        return title;
    }

    private static Button styledButton(String text, String style) {

        Button button = new Button(text);

        button.setStyle(style);

        return button;
    }

    public Parent getRoot() { return root; }

    public DatePicker getCheckInDate() { return checkInDate; }

    public DatePicker getCheckOutDate() { return checkOutDate; }

    public TextField getCheckInTime() { return checkInTime; }

    public TextField getCheckOutTime() { return checkOutTime; }

    public TextField getCustomerId() { return customerId; }

    public TextField getRoomNumber() { return roomNumber; }

    public TextField getCardId() { return cardId; }

    public TextField getServiceIds() { return serviceIds; }

    public TextField getPromotionCode() { return promotionCode; }

    public TextField getReservationId() { return reservationId; }

    public Spinner<Integer> getGuestCount() { return guestCount; }

    public TextArea getOutput() { return output; }

    public Button getAvailabilityButton() { return availabilityButton; }

    public Button getCreateButton() { return createButton; }

    public Button getCancelButton() { return cancelButton; }

    public Button getHistoryButton() { return historyButton; }
}