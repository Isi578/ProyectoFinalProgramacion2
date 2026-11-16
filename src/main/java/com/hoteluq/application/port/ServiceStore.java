package com.hoteluq.application.port;

import com.hoteluq.domain.model.ServiceOption;

/** Write side of the service catalog (ISP: reservation code only needs the read side). */
public interface ServiceStore extends ServiceCatalog {
    ServiceOption save(ServiceOption service);
}