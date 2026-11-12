package com.hoteluq.application.contract;

import com.hoteluq.domain.model.Room;

import java.util.List;

public interface RoomAdministrationUseCase {
    Room addRoom(Room room);
    Room updateRoom(Room room);
    void deactivateRoom(String roomNumber);
    List<Room> listRooms();
}