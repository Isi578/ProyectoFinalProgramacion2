package com.hoteluq.domain.loyalty;

import java.math.BigDecimal;

public interface PointsAccrualStrategy {
    int pointsFor(BigDecimal amountPaid);
}