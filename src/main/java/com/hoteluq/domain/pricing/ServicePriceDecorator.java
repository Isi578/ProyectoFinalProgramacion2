package com.hoteluq.domain.pricing;

import com.hoteluq.domain.model.ServiceOption;

import java.math.BigDecimal;
import java.util.List;

public final class ServicePriceDecorator implements PriceComponent {
    private final PriceComponent wrapped;
    private final List<ServiceOption> services;
    private final long nights;
    private final int guestCount;

    public ServicePriceDecorator(PriceComponent wrapped, List<ServiceOption> services,
                                 long nights, int guestCount) {
        this.wrapped = wrapped;
        this.services = List.copyOf(services);
        this.nights = nights;
        this.guestCount = guestCount;
    }

    @Override
    public BigDecimal amount() {
        BigDecimal serviceTotal = services.stream()
                .map(service -> service.priceFor(nights, guestCount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return wrapped.amount().add(serviceTotal);
    }
}