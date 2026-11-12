package com.hoteluq.domain.pricing;

import com.hoteluq.domain.model.Room;

import java.math.BigDecimal;

public final class RoomStayPrice implements PriceComponent {
    private final Room room;
    private final long nights;

    public RoomStayPrice(Room room, long nights) {
        if (room == null) throw new IllegalArgumentException("Room is required.");
        if (nights < 1) throw new IllegalArgumentException("A stay must include at least one night.");
        this.room = room;
        this.nights = nights;
    }

    @Override
    public BigDecimal amount() {
        return room.getRoomType().getNightlyRate().multiply(BigDecimal.valueOf(nights));
    }
}