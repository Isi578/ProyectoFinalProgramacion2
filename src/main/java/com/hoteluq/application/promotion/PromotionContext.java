package com.hoteluq.application.promotion;

import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.Room;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Facts about the reservation being priced; every condition in the chain reads from it. */
public record PromotionContext(Customer customer, Room room, LocalDateTime checkInAt,
                               LocalDateTime checkOutAt, long nights, BigDecimal subtotal) {
}