package com.hoteluq.infrastructure.factory;

import com.hoteluq.domain.model.Room;
import com.hoteluq.domain.model.RoomType;

import java.math.BigDecimal;

public final class SuiteRoomFactory extends RoomFactory {
    @Override
    public Room createRoom(String roomNumber, BigDecimal nightlyRate, int capacity) {
        RoomType type = new RoomType("SUITE", "Suite", nightlyRate);
        return new Room(roomNumber, type, capacity, true);
    }
}