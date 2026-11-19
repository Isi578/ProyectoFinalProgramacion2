package com.hoteluq.application.contract;

import com.hoteluq.domain.model.PaymentReceipt;

public interface CustomerServicePurchaseUseCase {
    PaymentReceipt purchaseService(String customerId, String cardId, String serviceId,
                                    long nights, int guestCount);
}
