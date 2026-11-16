package com.hoteluq.application.contract;

import com.hoteluq.domain.model.ServiceOption;

import java.util.List;

public interface ServiceAdministrationUseCase {
    ServiceOption addService(ServiceOption service);
    ServiceOption updateService(ServiceOption service);
    void deactivateService(String serviceId);
    List<ServiceOption> listServices();
}