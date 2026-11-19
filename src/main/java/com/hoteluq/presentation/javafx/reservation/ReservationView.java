package com.hoteluq.presentation.javafx.reservation;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import com.hoteluq.domain.model.ServiceOption;
import com.hoteluq.domain.model.Room;
import com.hoteluq.domain.model.VirtualCard;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private final ComboBox<Room> roomSelector = new ComboBox<>();

    private final ComboBox<VirtualCard> cardSelector = new ComboBox<>();

    private final VBox serviceChoices = new VBox(6);

    private final ScrollPane serviceChoicesScroll = new ScrollPane(serviceChoices);

    private final Label servicePriceSummary = new Label("Selecciona servicios opcionales.");

    private final Map<String, CheckBox> serviceCheckBoxes = new LinkedHashMap<>();

    private final Map<String, ServiceOption> servicesById = new LinkedHashMap<>();

    private long estimatedNights = 1;

    private int estimatedGuests = 1;

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
        roomSelector.setPromptText("Primero consulta disponibilidad");
        roomSelector.setMaxWidth(Double.MAX_VALUE);
        roomSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Room room) {
                if (room == null) return "";
                return "Habitación " + room.getNumber() + " | "
                        + room.getRoomType().getName() + " | "
                        + room.getRoomType().getNightlyRate().toPlainString()
                        + " COP/noche | capacidad " + room.getCapacity();
            }

            @Override
            public Room fromString(String value) { return null; }
        });
        cardSelector.setPromptText("Selecciona tu tarjeta virtual");
        cardSelector.setMaxWidth(Double.MAX_VALUE);
        cardSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(VirtualCard card) {
                if (card == null) return "";
                return card.getCardId() + (card.isActive() ? " (activa)" : " (inactiva)");
            }

            @Override
            public VirtualCard fromString(String value) { return null; }
        });
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
                label("Habitación disponible"), roomSelector,
                label("Tarjeta virtual"), cardSelector);

        serviceChoicesScroll.setFitToWidth(true);
        serviceChoicesScroll.setPrefViewportHeight(108);
        serviceChoicesScroll.setStyle("-fx-background:#ffffff;-fx-background-color:#ffffff;");
        serviceChoices.setPadding(new Insets(4));
        VBox servicesCard = new VBox(6,
                sectionTitle("Servicios adicionales opcionales"),
                serviceChoicesScroll,
                servicePriceSummary);
        servicesCard.setPadding(new Insets(8));
        servicesCard.setStyle(CARD_STYLE);

        form.addRow(row++, label("Código de promoción"), promotionCode);

        form.add(servicesCard, 0, row++, 4, 1);

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

        cardSelector.getItems().clear();

        reservationId.clear();

        roomSelector.getItems().clear();

        serviceCheckBoxes.values().forEach(checkBox -> checkBox.setSelected(false));
        updateServicePriceSummary();

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

        serviceCheckBoxes.values().forEach(checkBox -> checkBox.setDisable(disabled));

        cardSelector.setDisable(disabled);

        roomSelector.setDisable(disabled);
    }

    /** Displays the active catalog options and preserves current selections. */
    public void setAvailableServices(List<ServiceOption> services, long nights, int guests) {
        List<String> selectedIds = getSelectedServiceIds();
        serviceCheckBoxes.clear();
        servicesById.clear();
        serviceChoices.getChildren().clear();
        estimatedNights = Math.max(1, nights);
        estimatedGuests = Math.max(1, guests);

        List<ServiceOption> available = services == null ? List.of() : services.stream()
                .filter(ServiceOption::isAvailable)
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList();

        for (ServiceOption service : available) {
            CheckBox choice = new CheckBox();
            choice.setSelected(selectedIds.contains(service.getId()));
            choice.setOnAction(event -> updateServicePriceSummary());
            serviceCheckBoxes.put(service.getId(), choice);
            servicesById.put(service.getId(), service);
            serviceChoices.getChildren().add(choice);
        }

        if (available.isEmpty()) {
            serviceChoices.getChildren().add(label("No hay servicios disponibles actualmente."));
        }

        refreshServicePrices(nights, guests);
        updateServicePriceSummary();
    }

    /** Updates the per-stay estimates when dates or guest count change. */
    public void refreshServicePrices(long nights, int guests) {
        estimatedNights = Math.max(1, nights);
        estimatedGuests = Math.max(1, guests);
        NumberFormat cop = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));
        serviceCheckBoxes.forEach((id, checkBox) -> {
            ServiceOption service = servicesById.get(id);
            BigDecimal estimate = service.priceFor(estimatedNights, estimatedGuests);
            checkBox.setText(service.getName() + " — " + cop.format(estimate)
                    + " COP estimados");
        });
        updateServicePriceSummary();
    }

    public List<String> getSelectedServiceIds() {
        List<String> selected = new ArrayList<>();
        serviceCheckBoxes.forEach((id, checkBox) -> {
            if (checkBox.isSelected()) selected.add(id);
        });
        return List.copyOf(selected);
    }

    public void setCards(List<VirtualCard> cards) {
        List<VirtualCard> activeCards = cards == null ? List.of() : cards.stream()
                .filter(VirtualCard::isActive)
                .toList();
        cardSelector.getItems().setAll(activeCards);
        if (!activeCards.isEmpty()) cardSelector.setValue(activeCards.get(0));
    }

    public void setAvailableRooms(List<Room> rooms) {
        String previouslySelected = getSelectedRoomNumber();
        List<Room> available = rooms == null ? List.of() : List.copyOf(rooms);
        roomSelector.getItems().setAll(available);
        Room selection = available.stream()
                .filter(room -> room.getNumber().equals(previouslySelected))
                .findFirst()
                .orElse(null);
        roomSelector.setValue(selection);
    }

    public void clearAvailableRooms() {
        roomSelector.getItems().clear();
        roomSelector.setValue(null);
    }

    public String getSelectedRoomNumber() {
        Room selected = roomSelector.getValue();
        return selected == null ? "" : selected.getNumber();
    }

    public String getSelectedCardId() {
        VirtualCard selected = cardSelector.getValue();
        return selected == null ? "" : selected.getCardId();
    }

    public String getAvailableServiceNames() {
        return servicesById.values().stream()
                .map(ServiceOption::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .reduce((left, right) -> left + ", " + right)
                .orElse("ninguno por el momento");
    }

    private void updateServicePriceSummary() {
        NumberFormat cop = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));
        BigDecimal total = serviceCheckBoxes.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(entry -> servicesById.get(entry.getKey())
                        .priceFor(estimatedNights, estimatedGuests))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        servicePriceSummary.setText("Estimado de servicios seleccionados: "
                + cop.format(total) + " COP para " + estimatedNights
                + (estimatedNights == 1 ? " noche" : " noches") + " y "
                + estimatedGuests + (estimatedGuests == 1 ? " huésped." : " huéspedes."));
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

    public ComboBox<Room> getRoomSelector() { return roomSelector; }

    public ComboBox<VirtualCard> getCardSelector() { return cardSelector; }

    public TextField getPromotionCode() { return promotionCode; }

    public TextField getReservationId() { return reservationId; }

    public Spinner<Integer> getGuestCount() { return guestCount; }

    public TextArea getOutput() { return output; }

    public Button getAvailabilityButton() { return availabilityButton; }

    public Button getCreateButton() { return createButton; }

    public Button getCancelButton() { return cancelButton; }

    public Button getHistoryButton() { return historyButton; }
}
