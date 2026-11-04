package com.hoteluq.application.contract;

import java.math.BigDecimal;

public interface CardRechargeUseCase {
    void recharge(String administratorId, String cardId, BigDecimal amount);
}
