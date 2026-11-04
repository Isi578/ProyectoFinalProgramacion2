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

public final class ReservationView {
    private final BorderPane root = new BorderPane();
    private final DatePicker checkInDate = new DatePicker(LocalDate.now().plusDays(7));
    private final DatePicker checkOutDate = new DatePicker(LocalDate.now().plusDays(8));
    private final TextField checkInTime = new TextField("15:00");
    private final TextField checkOutTime = new TextField("11:00");
    private final TextField customerId = new TextField("GUEST-ANA-001");
    private final TextField roomNumber = new TextField();
    private final TextField cardId = new TextField("CARD-ANA-001");
    private final TextField serviceIds = new TextField();
    private final TextField promotionCode = new TextField();
    private final TextField reservationId = new TextField();
    private final Spinner<Integer> guestCount = new Spinner<>(1, 6, 1);
    private final TextArea output = new TextArea();
    private final Button availabilityButton = new Button("Check availability");
    private final Button createButton = new Button("Create reservation");
    private final Button cancelButton = new Button("Cancel reservation");
    private final Button historyButton = new Button("Refresh history");

    public ReservationView() {
        Label title = new Label("HotelUQ - Reservations");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        title.setPadding(new Insets(12, 12, 6, 12));

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new Insets(12));

        int row = 0;
        form.addRow(row++, new Label("Check-in date"), checkInDate,
                new Label("Time (HH:mm)"), checkInTime);
        form.addRow(row++, new Label("Check-out date"), checkOutDate,
                new Label("Time (HH:mm)"), checkOutTime);
        form.addRow(row++, new Label("Guest ID"), customerId,
                new Label("Guests"), guestCount);
        form.addRow(row++, new Label("Room number"), roomNumber,
                new Label("Card ID"), cardId);
        form.addRow(row++, new Label("Service IDs (comma separated)"), serviceIds,
                new Label("Promotion code"), promotionCode);
        form.addRow(row++, new Label("Reservation ID"), reservationId);

        HBox actions = new HBox(8, availabilityButton, createButton, cancelButton, historyButton);
        actions.setPadding(new Insets(0, 12, 12, 12));

        output.setEditable(false);
        output.setWrapText(true);
        output.setPrefRowCount(14);

        VBox content = new VBox(form, actions, output);
        root.setTop(title);
        root.setCenter(content);
        root.setPadding(new Insets(10));
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