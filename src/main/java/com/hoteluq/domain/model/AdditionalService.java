package com.hoteluq.domain.model;

import java.math.BigDecimal;

public final class AdditionalService implements ServiceOption {
    private final String id;
    private final String name;
    private final BigDecimal unitPrice;
    private final ServicePricingUnit pricingUnit;
    private final boolean available;

    public AdditionalService(String id, String name, BigDecimal unitPrice,
                             ServicePricingUnit pricingUnit, boolean available) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Service id is required.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Service name is required.");
        if (unitPrice == null || unitPrice.signum() < 0) throw new IllegalArgumentException("Service price cannot be negative.");
        if (pricingUnit == null) throw new IllegalArgumentException("Service pricing unit is required.");
        this.id = id.trim();
        this.name = name.trim();
        this.unitPrice = unitPrice;
        this.pricingUnit = pricingUnit;
        this.available = available;
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public boolean isAvailable() { return available; }

    @Override
    public BigDecimal priceFor(long nights, int guestCount) {
        if (nights < 1 || guestCount < 1) throw new IllegalArgumentException("Nights and guests must be positive.");
        return switch (pricingUnit) {
            case PER_STAY -> unitPrice;
            case PER_NIGHT -> unitPrice.multiply(BigDecimal.valueOf(nights));
            case PER_GUEST -> unitPrice.multiply(BigDecimal.valueOf(guestCount));
            case PER_GUEST_PER_NIGHT -> unitPrice.multiply(BigDecimal.valueOf(nights))
                    .multiply(BigDecimal.valueOf(guestCount));
        };
    }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public ServicePricingUnit getPricingUnit() { return pricingUnit; }

    @Override
    public String toString() {
        return name + " (" + unitPrice.toPlainString() + " COP, " + pricingUnit + ")";
    }
}