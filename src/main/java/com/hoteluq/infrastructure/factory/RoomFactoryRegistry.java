package com.hoteluq.infrastructure.factory;

import com.hoteluq.application.reservation.EntityNotFoundException;
import com.hoteluq.domain.model.Room;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RoomFactoryRegistry {
    private final Map<String, RoomFactory> factories = new ConcurrentHashMap<>();

    public void register(String type, RoomFactory factory) {
        if (type == null || type.isBlank() || factory == null) {
            throw new IllegalArgumentException("Room type and factory are required.");
        }
        factories.put(type.trim().toUpperCase(Locale.ROOT), factory);
    }

    public Room create(String type, String roomNumber, BigDecimal nightlyRate, int capacity) {
        if (type == null || type.isBlank()) throw new EntityNotFoundException("Room type is required.");
        RoomFactory factory = factories.get(type.trim().toUpperCase(Locale.ROOT));
        if (factory == null) throw new EntityNotFoundException("No room factory registered for type " + type);
        return factory.createRoom(roomNumber, nightlyRate, capacity);
    }
}