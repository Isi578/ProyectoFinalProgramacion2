package com.hoteluq.application.reservation;

import com.hoteluq.application.port.PromotionPolicy;
import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.Room;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class NoPromotionPolicy implements PromotionPolicy {
    @Override
    public BigDecimal apply(String promotionCode, Customer customer, Room room,
                            LocalDateTime checkInAt, LocalDateTime checkOutAt,
                            BigDecimal subtotal) {
        if (promotionCode != null && !promotionCode.isBlank()) {
            throw new PromotionRejectedException(
                    "Promotions are not configured yet; the supplied code cannot be validated.");
        }
        return subtotal;
    }
}