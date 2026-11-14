package com.hoteluq.application.service;

import com.hoteluq.application.contract.ServiceAdministrationUseCase;
import com.hoteluq.application.port.ServiceStore;
import com.hoteluq.domain.model.AdditionalService;
import com.hoteluq.domain.model.ServiceOption;
import com.hoteluq.domain.model.ServicePackage;

import java.util.ArrayList;
import java.util.List;

/**
 * Administrator operations on additional services. Packages hold their own copies of the
 * components, so every change is propagated to the packages that contain the service;
 * otherwise a deactivated service would still be sold inside a package.
 */
public final class ServiceAdministrationService implements ServiceAdministrationUseCase {
    private final ServiceStore store;

    public ServiceAdministrationService(ServiceStore store) { this.store = store; }

    @Override
    public synchronized ServiceOption addService(ServiceOption service) {
        requireService(service);
        if (store.findById(service.getId()).isPresent()) {
            throw new ServiceCatalogException("Service already exists: " + service.getId());
        }
        return store.save(service);
    }

    @Override
    public synchronized ServiceOption updateService(ServiceOption service) {
        requireService(service);
        requireExisting(service.getId());
        store.save(service);
        propagate(service);
        return service;
    }

    @Override
    public synchronized void deactivateService(String serviceId) {
        ServiceOption current = requireExisting(serviceId);
        if (current instanceof AdditionalService single) {
            AdditionalService inactive = new AdditionalService(single.getId(), single.getName(),
                    single.getUnitPrice(), single.getPricingUnit(), false);
            store.save(inactive);
            propagate(inactive);
        } else {
            throw new ServiceCatalogException("A package is unavailable when any of its components is; "
                    + "deactivate the component instead of the package " + serviceId + ".");
        }
    }

    @Override
    public List<ServiceOption> listServices() { return store.findAll(); }

    private void propagate(ServiceOption replacement) {
        for (ServiceOption existing : store.findAll()) {
            ServiceOption rebuilt = replaceIn(existing, replacement);
            if (rebuilt != existing) store.save(rebuilt);
        }
    }

    private static ServiceOption replaceIn(ServiceOption node, ServiceOption replacement) {
        if (node.getId().equals(replacement.getId())) return replacement;
        if (!(node instanceof ServicePackage servicePackage)) return node;

        boolean changed = false;
        List<ServiceOption> components = new ArrayList<>();
        for (ServiceOption component : servicePackage.getComponents()) {
            ServiceOption rebuilt = replaceIn(component, replacement);
            changed |= rebuilt != component;
            components.add(rebuilt);
        }
        return changed
                ? ServicePackage.builder()
                .id(servicePackage.getId())
                .name(servicePackage.getName())
                .addAll(components)
                .build()
                : node;
    }

    private ServiceOption requireExisting(String serviceId) {
        return store.findById(serviceId)
                .orElseThrow(() -> new ServiceCatalogException("Service not found: " + serviceId));
    }

    private static void requireService(ServiceOption service) {
        if (service == null) throw new ServiceCatalogException("Service is required.");
    }
}