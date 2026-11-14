package com.hoteluq.application.promotion;

import com.hoteluq.application.reservation.PromotionRejectedException;

import java.util.Set;

/** The reserved room must be one of the room types the promotion covers. */
public final class RoomTypeCondition implements PromotionCondition {
    private final Set<String> roomTypeIds;

    public RoomTypeCondition(Set<String> roomTypeIds) {
        if (roomTypeIds == null || roomTypeIds.isEmpty()) {
            throw new IllegalArgumentException("At least one room type is required.");
        }
        this.roomTypeIds = Set.copyOf(roomTypeIds);
    }

    @Override
    public void check(PromotionContext context) {
        if (!roomTypeIds.contains(context.room().getRoomType().getId())) {
            throw new PromotionRejectedException("The promotion does not apply to "
                    + context.room().getRoomType().getName() + " rooms.");
        }
    }
}