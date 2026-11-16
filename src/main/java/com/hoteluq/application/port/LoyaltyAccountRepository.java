package com.hoteluq.application.port;

import com.hoteluq.domain.loyalty.LoyaltyAccount;

public interface LoyaltyAccountRepository {
    LoyaltyAccount findOrCreate(String customerId);
}