package com.hoteluq.infrastructure.factory;

import com.hoteluq.domain.model.Room;
import com.hoteluq.domain.model.RoomType;

import java.math.BigDecimal;

public final class StandardRoomFactory extends RoomFactory {
    @Override
    public Room createRoom(String roomNumber, BigDecimal nightlyRate, int capacity) {
        RoomType type = new RoomType("STANDARD", "Standard", nightlyRate);
        return new Room(roomNumber, type, capacity, true);
    }
}