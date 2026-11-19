package com.hoteluq.presentation.javafx.auth;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * CustomerAuthView (vista de acceso de clientes).
 *
 * Construye los elementos JavaFX del inicio de sesión,
 * el registro y el resumen de la sesión.
 *
 * No valida datos ni llama a los servicios.
 *
 * La lógica de los botones se encuentra
 * en CustomerAuthController.
 */
public final class CustomerAuthView {

    // root (raíz): contenedor principal de la vista.
    private final StackPane root = new StackPane();

    // Campos del inicio de sesión.
    final TextField loginEmail = field("Correo electrónico");

    final PasswordField loginPassword =
            password("Contraseña");

    final Button login = primaryButton("Iniciar sesión");

    final Hyperlink goToRegister =
            new Hyperlink("¿No tienes cuenta? Regístrate");

    // Campos del registro.
    final TextField registerName = field("Nombre completo");

    final TextField registerEmail =
            field("Correo electrónico");

    final PasswordField registerPassword =
            password("Contraseña (mínimo 8 caracteres)");

    final PasswordField registerConfirmation =
            password("Confirmar contraseña");

    final Button register = primaryButton("Crear cuenta");

    final Hyperlink goToLogin =
            new Hyperlink("¿Ya tienes cuenta? Inicia sesión");

    // Resumen de la sesión iniciada.
    final Label sessionTitle = new Label();

    final Label sessionDetail = label("");

    final Button logout = new Button("Cerrar sesión");

    // Mensaje de error o éxito.
    final Label message = new Label();

    // Paneles que se alternan dentro de la tarjeta.
    private final VBox loginPane;

    private final VBox registerPane;

    private final VBox sessionPane;

    /**
     * Constructor de CustomerAuthView.
     *
     * Organiza una tarjeta centrada con tres paneles:
     * inicio de sesión, registro y sesión activa.
     */
    public CustomerAuthView() {

        Label heading = new Label(
                "HotelUQ | Cuenta de cliente"
        );

        heading.setStyle(
                "-fx-font-size:22px;"
                        + "-fx-font-weight:bold;"
                        + "-fx-text-fill:#173f35;"
        );

        loginPane = new VBox(
                10,
                title("Iniciar sesión"),
                loginEmail,
                loginPassword,
                login,
                goToRegister
        );

        registerPane = new VBox(
                10,
                title("Crear cuenta de cliente"),
                registerName,
                registerEmail,
                registerPassword,
                registerConfirmation,
                register,
                goToLogin
        );

        sessionTitle.setStyle(
                "-fx-font-size:15px;"
                        + "-fx-font-weight:bold;"
                        + "-fx-text-fill:#173f35;"
        );

        sessionPane = new VBox(
                10,
                sessionTitle,
                sessionDetail,
                logout
        );

        message.setWrapText(true);

        StackPane panes = new StackPane(
                loginPane,
                registerPane,
                sessionPane
        );

        VBox card = new VBox(
                14,
                heading,
                panes,
                message
        );

        card.setPadding(new Insets(24));

        card.setMaxWidth(400);

        card.setMaxHeight(Region.USE_PREF_SIZE);

        card.setStyle(
                "-fx-background-color:white;"
                        + "-fx-border-color:#d3dfda;"
        );

        root.setAlignment(Pos.CENTER);

        root.setPadding(new Insets(20));

        root.setStyle(
                "-fx-background-color:#ecf5f2;"
        );

        root.getChildren().add(card);

        showLogin();
    }

    /**
     * showLogin (mostrar inicio de sesión).
     */
    public void showLogin() {

        display(loginPane);

        clearMessage();
    }

    /**
     * showRegister (mostrar registro).
     */
    public void showRegister() {

        display(registerPane);

        clearMessage();
    }

    /**
     * showSession (mostrar sesión activa).
     *
     * Muestra los datos del cliente que ingresó.
     */
    public void showSession(
            String fullName,
            String email,
            String customerId) {

        sessionTitle.setText("Sesión iniciada: " + fullName);

        sessionDetail.setText(
                email
                        + "\nID de cliente: "
                        + customerId
                        + "\nEl ID se copió al módulo de reservas."
        );

        display(sessionPane);
    }

    /**
     * showSuccess (mostrar mensaje de éxito).
     */
    public void showSuccess(String text) {

        message.setText(text);

        message.setStyle(
                "-fx-text-fill:#176b57;"
                        + "-fx-font-weight:bold;"
        );
    }

    /**
     * showError (mostrar mensaje de error).
     */
    public void showError(String text) {

        message.setText(text);

        message.setStyle(
                "-fx-text-fill:#a12a2a;"
        );
    }

    /**
     * clearMessage (limpiar mensaje).
     */
    public void clearMessage() {

        message.setText("");
    }

    /**
     * clearPasswords (limpiar contraseñas).
     *
     * Evita dejar contraseñas escritas en pantalla.
     */
    public void clearPasswords() {

        loginPassword.clear();

        registerPassword.clear();

        registerConfirmation.clear();
    }

    /**
     * clearRegisterForm (limpiar formulario de registro).
     */
    public void clearRegisterForm() {

        registerName.clear();

        registerEmail.clear();

        clearPasswords();
    }

    /**
     * clearLoginForm (limpiar formulario de ingreso).
     */
    public void clearLoginForm() {

        loginEmail.clear();

        loginPassword.clear();
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
     * display (mostrar un panel).
     *
     * Deja visible solo el panel indicado.
     */
    private void display(Node visiblePane) {

        for (Node pane : new Node[] {
                loginPane, registerPane, sessionPane}) {

            boolean visible = pane == visiblePane;

            pane.setVisible(visible);

            pane.setManaged(visible);
        }
    }

    private static Label title(String text) {

        Label title = new Label(text);

        title.setStyle(
                "-fx-font-size:15px;"
                        + "-fx-font-weight:bold;"
                        + "-fx-text-fill:#173f35;"
        );

        return title;
    }

    private static TextField field(String placeholder) {

        TextField field = new TextField();

        field.setPromptText(placeholder);

        return field;
    }

    private static PasswordField password(String placeholder) {

        PasswordField field = new PasswordField();

        field.setPromptText(placeholder);

        return field;
    }

    private static Button primaryButton(String text) {

        Button button = new Button(text);

        button.setMaxWidth(Double.MAX_VALUE);

        button.setStyle(
                "-fx-background-color:#176b57;"
                        + "-fx-text-fill:white;"
                        + "-fx-font-weight:bold;"
        );

        return button;
    }

    private static Label label(String message) {

        Label label = new Label(message);

        label.setStyle(
                "-fx-text-fill:#335f55;"
                        + "-fx-font-size:13px;"
        );

        return label;
    }
}