package com.hoteluq.presentation.javafx.auth;

import com.hoteluq.application.contract.CustomerAccountUseCase;
import com.hoteluq.application.customer.InvalidCredentialsException;
import com.hoteluq.domain.model.Customer;

import java.util.function.Consumer;

/**
 * CustomerAuthController (controlador de acceso de clientes).
 *
 * Conecta los botones de CustomerAuthView
 * con CustomerAccountUseCase.
 *
 * No guarda ni cifra contraseñas: esa responsabilidad
 * es de CustomerAccountService.
 */
public final class CustomerAuthController {

    // Mensaje que lanza el servicio cuando el correo ya existe.
    private static final String DUPLICATE_EMAIL_MARKER =
            "already exists";

    // Referencia a la vista.
    private final CustomerAuthView view;

    // Contrato de cuentas de clientes.
    private final CustomerAccountUseCase accounts;

    // Acción que se ejecuta cuando un cliente ingresa.
    private final Consumer<Customer> onAuthenticated;

    // Acción que se ejecuta cuando el cliente cierra sesión.
    private final Runnable onLogout;

    /**
     * Constructor de CustomerAuthController.
     *
     * @param onAuthenticated acción para otros módulos
     *                        (puede ser null)
     */
    public CustomerAuthController(
            CustomerAuthView view,
            CustomerAccountUseCase accounts,
            Consumer<Customer> onAuthenticated) {

        this(view, accounts, onAuthenticated, null);
    }

    /**
     * Constructor de CustomerAuthController con cierre de sesión.
     *
     * @param onAuthenticated acción al iniciar sesión (puede ser null)
     * @param onLogout        acción al cerrar sesión (puede ser null)
     */
    public CustomerAuthController(
            CustomerAuthView view,
            CustomerAccountUseCase accounts,
            Consumer<Customer> onAuthenticated,
            Runnable onLogout) {

        this.view = view;
        this.accounts = accounts;
        this.onAuthenticated =
                onAuthenticated == null
                        ? customer -> { }
                        : onAuthenticated;
        this.onLogout =
                onLogout == null
                        ? () -> { }
                        : onLogout;
    }

    /**
     * initialize (inicializar).
     *
     * Asigna una acción a cada botón.
     */
    public void initialize() {

        view.login.setOnAction(event -> login());

        view.loginPassword.setOnAction(event -> login());

        view.register.setOnAction(event -> register());

        view.registerConfirmation.setOnAction(
                event -> register()
        );

        view.goToRegister.setOnAction(
                event -> view.showRegister()
        );

        view.goToLogin.setOnAction(
                event -> view.showLogin()
        );

        view.logout.setOnAction(event -> logout());
    }

    /**
     * login (iniciar sesión).
     *
     * Valida el formulario y autentica al cliente.
     */
    private void login() {

        String email = view.loginEmail.getText();

        try {

            CustomerAuthValidator.validateLogin(
                    email,
                    view.loginPassword.getText()
            );

        } catch (IllegalArgumentException invalidForm) {

            view.showError(invalidForm.getMessage());

            return;
        }

        try {

            Customer customer = accounts.authenticate(
                    email,
                    view.loginPassword.getText()
            );

            view.clearLoginForm();

            view.showSession(
                    customer.getFullName(),
                    customer.getEmail(),
                    customer.getId()
            );

            view.showSuccess("Inicio de sesión correcto.");

            onAuthenticated.accept(customer);

        } catch (InvalidCredentialsException error) {

            view.loginPassword.clear();

            view.showError(
                    "Correo o contraseña incorrectos."
            );

        } catch (RuntimeException error) {

            view.loginPassword.clear();

            view.showError(
                    "No se pudo iniciar sesión: "
                            + error.getMessage()
            );
        }
    }

    /**
     * register (registrar).
     *
     * Valida el formulario y crea la cuenta del cliente.
     */
    private void register() {

        try {

            CustomerAuthValidator.validateRegistration(
                    view.registerName.getText(),
                    view.registerEmail.getText(),
                    view.registerPassword.getText(),
                    view.registerConfirmation.getText()
            );

        } catch (IllegalArgumentException invalidForm) {

            view.showError(invalidForm.getMessage());

            return;
        }

        try {

            Customer customer = accounts.register(
                    view.registerName.getText(),
                    view.registerEmail.getText(),
                    view.registerPassword.getText()
            );

            view.clearRegisterForm();

            view.showLogin();

            view.loginEmail.setText(customer.getEmail());

            view.showSuccess(
                    "Cuenta creada para "
                            + customer.getFullName()
                            + ". Ya puedes iniciar sesión."
            );

        } catch (RuntimeException error) {

            view.registerPassword.clear();

            view.registerConfirmation.clear();

            view.showError(registrationMessage(error));
        }
    }

    /**
     * logout (cerrar sesión).
     */
    private void logout() {

        view.clearLoginForm();

        view.showLogin();

        view.showSuccess("Sesión cerrada.");

        onLogout.run();
    }

    /**
     * registrationMessage (mensaje de registro).
     *
     * Traduce los errores del servicio al idioma de la interfaz.
     */
    private static String registrationMessage(
            RuntimeException error) {

        String detail = error.getMessage();

        if (detail != null
                && detail.contains(DUPLICATE_EMAIL_MARKER)) {

            return "Ya existe un cliente con este correo.";
        }

        return "No se pudo crear la cuenta: " + detail;
    }
}