package hoteluq.application.customer;


import com.hoteluq.presentation.javafx.auth.CustomerAuthValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerAuthValidatorTest {

    @Test
    void acceptsValidLoginData() {
        assertDoesNotThrow(() ->
                CustomerAuthValidator.validateLogin("ana@example.com", "secret123"));
    }

    @Test
    void rejectsLoginWithMissingOrInvalidData() {
        assertEquals("El correo electrónico es obligatorio.",
                assertThrows(IllegalArgumentException.class,
                        () -> CustomerAuthValidator.validateLogin(" ", "secret123")).getMessage());
        assertEquals("El correo electrónico no es válido.",
                assertThrows(IllegalArgumentException.class,
                        () -> CustomerAuthValidator.validateLogin("no-es-correo", "secret123")).getMessage());
        assertEquals("La contraseña es obligatoria.",
                assertThrows(IllegalArgumentException.class,
                        () -> CustomerAuthValidator.validateLogin("ana@example.com", "")).getMessage());
    }

    @Test
    void acceptsValidRegistrationData() {
        assertDoesNotThrow(() -> CustomerAuthValidator.validateRegistration(
                "Isabela Quintero", "isabela@example.com", "secret123", "secret123"));
    }

    @Test
    void rejectsRegistrationWithMissingName() {
        assertThrows(IllegalArgumentException.class,
                () -> CustomerAuthValidator.validateRegistration(
                        " ", "isabela@example.com", "secret123", "secret123"));
    }

    @Test
    void rejectsRegistrationWithShortPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> CustomerAuthValidator.validateRegistration(
                        "Isabela", "isabela@example.com", "short", "short"));
    }

    @Test
    void rejectsRegistrationWhenPasswordsDoNotMatch() {
        assertEquals("Las contraseñas no coinciden.",
                assertThrows(IllegalArgumentException.class,
                        () -> CustomerAuthValidator.validateRegistration(
                                "Isabela", "isabela@example.com", "secret123", "secret124"))
                        .getMessage());
    }
}