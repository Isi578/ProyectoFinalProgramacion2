package com.hoteluq.application.port;

import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.Room;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PromotionPolicy {
    BigDecimal apply(String promotionCode, Customer customer, Room room,
                     LocalDateTime checkInAt, LocalDateTime checkOutAt,
                     BigDecimal subtotal);
}