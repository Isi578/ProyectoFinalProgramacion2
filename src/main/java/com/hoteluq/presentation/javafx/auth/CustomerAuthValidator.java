package com.hoteluq.presentation.javafx.auth;

import java.util.regex.Pattern;

/**
 * CustomerAuthValidator (validador del acceso de clientes).
 *
 * Revisa los datos escritos en los formularios
 * antes de enviarlos al servicio de cuentas.
 *
 * Las reglas de negocio (correo repetido, contraseña
 * correcta) siguen estando en CustomerAccountService.
 *
 * No depende de JavaFX, por lo que se puede probar solo.
 */
public final class CustomerAuthValidator {

    // Longitud mínima de contraseña para cuentas nuevas.
    public static final int MIN_PASSWORD_LENGTH = 8;

    // Mismo formato de correo que valida CustomerAccountService.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private CustomerAuthValidator() {
    }

    /**
     * validateLogin (validar inicio de sesión).
     *
     * @throws IllegalArgumentException con un mensaje en español
     */
    public static void validateLogin(String email, String password) {

        requireEmail(email);

        if (isBlank(password)) {
            throw new IllegalArgumentException(
                    "La contraseña es obligatoria."
            );
        }
    }

    /**
     * validateRegistration (validar registro).
     *
     * @throws IllegalArgumentException con un mensaje en español
     */
    public static void validateRegistration(
            String fullName,
            String email,
            String password,
            String confirmation) {

        if (isBlank(fullName)) {
            throw new IllegalArgumentException(
                    "El nombre completo es obligatorio."
            );
        }

        requireEmail(email);

        if (isBlank(password)) {
            throw new IllegalArgumentException(
                    "La contraseña es obligatoria."
            );
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos "
                            + MIN_PASSWORD_LENGTH
                            + " caracteres."
            );
        }

        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Las contraseñas no coinciden."
            );
        }
    }

    private static void requireEmail(String email) {

        if (isBlank(email)) {
            throw new IllegalArgumentException(
                    "El correo electrónico es obligatorio."
            );
        }

        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException(
                    "El correo electrónico no es válido."
            );
        }
    }

    private static boolean isBlank(String value) {

        return value == null || value.isBlank();
    }
}
