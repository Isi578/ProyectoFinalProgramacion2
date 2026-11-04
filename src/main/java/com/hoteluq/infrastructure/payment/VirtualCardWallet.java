package com.hoteluq.infrastructure.payment;

import java.math.BigDecimal;
import java.util.List;

public interface VirtualCardWallet {
    void registerCard(String cardId, String customerId, BigDecimal openingBalance, boolean active);
    WalletTransaction charge(String cardId, String customerId, BigDecimal amount);
    WalletTransaction refund(String originalChargeId, BigDecimal amount);
    WalletTransaction rechargeByAdministrator(String cardId, BigDecimal amount, String administratorId);
    BigDecimal getBalance(String cardId);
    List<WalletTransaction> getMovements(String cardId);
}