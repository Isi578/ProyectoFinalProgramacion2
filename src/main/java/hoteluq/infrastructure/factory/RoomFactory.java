package com.hoteluq.infrastructure.factory;

import com.hoteluq.domain.model.Room;

import java.math.BigDecimal;

public abstract class RoomFactory {
    public abstract Room createRoom(String roomNumber, BigDecimal nightlyRate, int capacity);
}