package com.hoteluq.application.port;

import java.util.Optional;

public interface CustomerCredentialRepository {
    void save(String customerId, String salt, String passwordHash);
    Optional<Credential> findByCustomerId(String customerId);
    void deleteByCustomerId(String customerId);

    record Credential(String salt, String passwordHash) { }
}
