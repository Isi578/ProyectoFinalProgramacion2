package com.hoteluq.application.contract;

import com.hoteluq.domain.model.PaymentMovement;
import com.hoteluq.domain.model.VirtualCard;

import java.math.BigDecimal;
import java.util.List;

/** Customer-scoped operations for virtual cards. */
public interface CustomerVirtualCardUseCase {
    VirtualCard issueForCustomer(String customerId);
    List<VirtualCard> findCardsForCustomer(String customerId);
    BigDecimal getCustomerCardBalance(String customerId, String cardId);
    List<PaymentMovement> getCustomerCardMovements(String customerId, String cardId);
}
