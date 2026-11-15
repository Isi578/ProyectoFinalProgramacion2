package com.hoteluq.domain.loyalty;

import java.math.BigDecimal;

public record LoyaltyBenefit(String id, String name, BenefitType type,
                             int pointsCost, BigDecimal value) {
    public LoyaltyBenefit {
        if (id == null || id.isBlank()) throw new LoyaltyException("Benefit id is required.");
        if (name == null || name.isBlank()) throw new LoyaltyException("Benefit name is required.");
        if (type == null) throw new LoyaltyException("Benefit type is required.");
        if (pointsCost < 1) throw new LoyaltyException("Benefit cost must be positive.");
        if (value == null || value.signum() <= 0) throw new LoyaltyException("Benefit value must be positive.");
    }
}