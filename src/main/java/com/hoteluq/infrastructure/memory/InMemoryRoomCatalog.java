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

    public void add(Room room) { rooms.put(room.getNumber(), room); }

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