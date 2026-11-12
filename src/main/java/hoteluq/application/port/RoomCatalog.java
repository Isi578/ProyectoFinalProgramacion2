package com.hoteluq.application.port;

import com.hoteluq.domain.model.Room;

import java.util.List;
import java.util.Optional;

public interface RoomCatalog {
    List<Room> findActiveRooms();
    Optional<Room> findActiveRoom(String roomNumber);
}