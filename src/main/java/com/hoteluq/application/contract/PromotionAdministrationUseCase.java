package com.hoteluq.application.contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PromotionAdministrationUseCase {
    void createPromotion(String code, BigDecimal discountRate,
                         LocalDateTime startsAt, LocalDateTime endsAt);
    void deactivatePromotion(String code);
}