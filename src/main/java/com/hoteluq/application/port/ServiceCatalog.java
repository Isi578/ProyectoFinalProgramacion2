package com.hoteluq.application.port;

import com.hoteluq.domain.model.ServiceOption;

import java.util.List;
import java.util.Optional;

public interface ServiceCatalog {
    Optional<ServiceOption> findById(String serviceId);
    List<ServiceOption> findAll();
}