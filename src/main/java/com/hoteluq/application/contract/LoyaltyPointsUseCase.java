package com.hoteluq.application.contract;

import com.hoteluq.domain.loyalty.LoyaltyBenefit;
import com.hoteluq.domain.loyalty.RedemptionVoucher;

import java.util.List;

public interface LoyaltyPointsUseCase {
    int getAvailablePoints(String customerId);
    void redeemPoints(String customerId, int points, String benefitId);
    List<String> getPointHistory(String customerId);
    List<LoyaltyBenefit> listBenefits();
    RedemptionVoucher redeemBenefit(String customerId, String benefitId);
}
