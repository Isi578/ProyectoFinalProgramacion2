package com.hoteluq.application.contract;

import com.hoteluq.infrastructure.payment.WalletTransaction;

import java.math.BigDecimal;

public interface CardRechargeUseCase {
    WalletTransaction recharge(String administratorId, String cardId, BigDecimal amount);
}