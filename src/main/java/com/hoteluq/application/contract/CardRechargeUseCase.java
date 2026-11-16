package com.hoteluq.application.contract;

import java.math.BigDecimal;

public interface CardRechargeUseCase {
    void recharge(String administratorId, String cardId, BigDecimal amount);

    default void recharge(String idempotencyKey, String administratorId,
                          String cardId, BigDecimal amount) {
        recharge(administratorId, cardId, amount);
    }
}
