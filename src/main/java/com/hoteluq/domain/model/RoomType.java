package com.hoteluq.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class RoomType {
    private final String id;
    private final String name;
    private final BigDecimal nightlyRate;

    public RoomType(String id, String name, BigDecimal nightlyRate) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Room type id is required.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Room type name is required.");
        if (nightlyRate == null || nightlyRate.signum() <= 0) throw new IllegalArgumentException("Nightly rate must be positive.");
        this.id = id.trim();
        this.name = name.trim();
        this.nightlyRate = nightlyRate;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getNightlyRate() { return nightlyRate; }

    @Override
    public boolean equals(Object other) {
        return other instanceof RoomType type && id.equals(type.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}