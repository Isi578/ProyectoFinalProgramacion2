package com.hoteluq.domain.model;

public final class Room {
    private final String number;
    private final RoomType roomType;
    private final int capacity;
    private final boolean active;

    public Room(String number, RoomType roomType, int capacity, boolean active) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("Room number is required.");
        if (roomType == null) throw new IllegalArgumentException("Room type is required.");
        if (capacity < 1) throw new IllegalArgumentException("Room capacity must be positive.");
        this.number = number.trim();
        this.roomType = roomType;
        this.capacity = capacity;
        this.active = active;
    }

    public String getNumber() { return number; }
    public RoomType getRoomType() { return roomType; }
    public int getCapacity() { return capacity; }
    public boolean isActive() { return active; }

    @Override
    public String toString() {
        return "Room " + number + " - " + roomType.getName()
                + " - " + roomType.getNightlyRate().toPlainString() + " COP/night"
                + " - capacity " + capacity;
    }
}