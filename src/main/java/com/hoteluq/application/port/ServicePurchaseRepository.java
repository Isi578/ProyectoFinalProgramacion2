package com.hoteluq.application.port;

import com.hoteluq.domain.model.ServicePurchase;
import java.util.List;

/** Historial de servicios comprados sin reservar una habitación. */
public interface ServicePurchaseRepository {
    void save(ServicePurchase purchase);
    List<ServicePurchase> findAll();
}
