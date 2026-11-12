package com.hoteluq.application.customer;

import com.hoteluq.application.contract.CustomerAccountUseCase;
import com.hoteluq.application.port.CustomerCatalog;
import com.hoteluq.application.port.CustomerCredentialRepository;
import com.hoteluq.domain.model.Customer;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class CustomerAccountService implements CustomerAccountUseCase {
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH_BITS = 256;
    private static final int HASH_ITERATIONS = 120_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final CustomerCatalog customerCatalog;
    private final CustomerCredentialRepository credentialRepository;

    public CustomerAccountService(
            CustomerCatalog customerCatalog,
            CustomerCredentialRepository credentialRepository) {
        if (customerCatalog == null || credentialRepository == null) {
            throw new IllegalArgumentException("Customer catalog and credentials are required.");
        }
        this.customerCatalog = customerCatalog;
        this.credentialRepository = credentialRepository;
    }

    @Override
    public synchronized Customer register(String fullName, String email, String password) {
        String normalizedName = requireText(fullName, "Customer name");
        String normalizedEmail = normalizeAndValidateEmail(email);
        requireText(password, "Password");

        if (customerCatalog.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("A customer with this email already exists.");
        }

        Customer customer = new Customer(
                UUID.randomUUID().toString(), normalizedName, normalizedEmail);
        String salt = newSalt();
        String passwordHash = hashPassword(password, salt);
        credentialRepository.save(customer.getId(), salt, passwordHash);
        try {
            customerCatalog.save(customer);
        } catch (RuntimeException saveFailure) {
            credentialRepository.deleteByCustomerId(customer.getId());
            throw saveFailure;
        }
        return customer;
    }

    public synchronized void initializeCredentialsForExistingCustomer(
            String customerId, String password) {
        String normalizedCustomerId = requireText(customerId, "Customer id");
        requireText(password, "Password");
        Customer customer = customerCatalog.findById(normalizedCustomerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer does not exist."));
        if (credentialRepository.findByCustomerId(customer.getId()).isPresent()) {
            throw new IllegalArgumentException("Customer credentials are already initialized.");
        }

        String salt = newSalt();
        credentialRepository.save(customer.getId(), salt, hashPassword(password, salt));
    }

    @Override
    public Customer authenticate(String email, String password) {
        String normalizedEmail = normalizeAndValidateEmail(email);
        requireText(password, "Password");

        Customer customer = customerCatalog.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException());
        CustomerCredentialRepository.Credential credential =
                credentialRepository.findByCustomerId(customer.getId())
                        .orElseThrow(() -> new InvalidCredentialsException());

        byte[] expectedHash = Base64.getDecoder().decode(credential.passwordHash());
        byte[] actualHash = Base64.getDecoder().decode(hashPassword(password, credential.salt()));
        if (!MessageDigest.isEqual(expectedHash, actualHash)) {
            throw new InvalidCredentialsException();
        }
        return customer;
    }

    private static String normalizeAndValidateEmail(String email) {
        String normalizedEmail = requireText(email, "Email").toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("Email is invalid.");
        }
        return normalizedEmail;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private static String newSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private static String hashPassword(String password, String salt) {
        PBEKeySpec keySpec = new PBEKeySpec(
                password.toCharArray(),
                Base64.getDecoder().decode(salt),
                HASH_ITERATIONS,
                HASH_LENGTH_BITS);
        try {
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not securely process customer password.", exception);
        } finally {
            keySpec.clearPassword();
        }
    }
}
