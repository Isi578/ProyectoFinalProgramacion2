package com.hoteluq.application.port;

import java.math.BigDecimal;

public interface LoyaltyPointsPort {
    void recordEligibleSpend(String customerId, String reservationId, BigDecimal amountPaid);
}