package com.hoteluq.application.contract;

import com.hoteluq.domain.loyalty.LoyaltyBenefit;
import com.hoteluq.domain.loyalty.RedemptionVoucher;

import java.util.List;

public interface LoyaltyPointsUseCase {
    int getAvailablePoints(String customerId);

    /** Consume puntos para una operación identificada de forma única. */
    void spendForPayment(String customerId, int points, String operationId);

    /** Restituye puntos si falla el cobro o si procede una devolución. */
    void restorePayment(String customerId, int points, String operationId);
    void redeemPoints(String customerId, int points, String benefitId);
    List<String> getPointHistory(String customerId);
    List<LoyaltyBenefit> listBenefits();
    RedemptionVoucher redeemBenefit(String customerId, String benefitId);
}
