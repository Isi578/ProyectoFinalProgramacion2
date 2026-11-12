package com.hoteluq.application.contract;

import java.util.List;

public interface LoyaltyPointsUseCase {
    int getAvailablePoints(String customerId);
    void redeemPoints(String customerId, int points, String benefitId);
    List<String> getPointHistory(String customerId);
}