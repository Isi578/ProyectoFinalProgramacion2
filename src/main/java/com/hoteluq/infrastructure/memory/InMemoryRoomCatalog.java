package com.hoteluq.infrastructure.memory;

import com.hoteluq.application.port.RoomCatalog;
import com.hoteluq.domain.model.Room;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryRoomCatalog implements RoomCatalog {
    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

    public InMemoryRoomCatalog(List<Room> initialRooms) {
        if (initialRooms != null) initialRooms.forEach(this::add);
    }

    /** Rechaza números repetidos para no sobrescribir una habitación accidentalmente. */
    public synchronized void add(Room room) {
        if (rooms.putIfAbsent(room.getNumber(), room) != null) {
            throw new IllegalArgumentException("Ya existe la habitación " + room.getNumber());
        }
    }

    /** Actualiza datos y permite cambiar el estado sin perder reservas históricas. */
    public synchronized void update(Room room) {
        if (!rooms.containsKey(room.getNumber())) {
            throw new IllegalArgumentException("La habitación no existe: " + room.getNumber());
        }
        rooms.put(room.getNumber(), room);
    }

    public List<Room> findAll() {
        return rooms.values().stream().sorted((a,b) -> a.getNumber().compareTo(b.getNumber())).toList();
    }

    @Override
    public List<Room> findActiveRooms() {
        return rooms.values().stream().filter(Room::isActive)
                .sorted((left, right) -> left.getNumber().compareTo(right.getNumber()))
                .toList();
    }

    @Override
    public Optional<Room> findActiveRoom(String roomNumber) {
        Room room = rooms.get(roomNumber);
        return room != null && room.isActive() ? Optional.of(room) : Optional.empty();
    }
}
