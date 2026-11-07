package com.hoteluq.domain.model;

import java.math.BigDecimal;

public interface ServiceOption {
    String getId();
    String getName();
    boolean isAvailable();
    BigDecimal priceFor(long nights, int guestCount);
}