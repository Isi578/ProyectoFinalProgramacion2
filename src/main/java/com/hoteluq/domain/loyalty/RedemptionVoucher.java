package com.hoteluq.domain.loyalty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class RedemptionVoucher {
    private final String code;
    private final String customerId;
    private final String benefitId;
    private final BigDecimal value;
    private final LocalDateTime issuedAt;
    private boolean used;

    public RedemptionVoucher(String code, String customerId, String benefitId,
                             BigDecimal value, LocalDateTime issuedAt) {
        this.code = code;
        this.customerId = customerId;
        this.benefitId = benefitId;
        this.value = value;
        this.issuedAt = issuedAt;
    }

    public void markUsed() {
        if (used) throw new LoyaltyException("Voucher " + code + " was already used.");
        used = true;
    }

    public String getCode() { return code; }
    public String getCustomerId() { return customerId; }
    public String getBenefitId() { return benefitId; }
    public BigDecimal getValue() { return value; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public boolean isUsed() { return used; }
}