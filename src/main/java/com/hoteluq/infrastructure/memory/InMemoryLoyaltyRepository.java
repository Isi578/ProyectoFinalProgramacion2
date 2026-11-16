package com.hoteluq.infrastructure.memory;

import com.hoteluq.application.port.LoyaltyAccountRepository;
import com.hoteluq.application.port.VoucherRepository;
import com.hoteluq.domain.loyalty.LoyaltyAccount;
import com.hoteluq.domain.loyalty.RedemptionVoucher;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryLoyaltyRepository implements LoyaltyAccountRepository, VoucherRepository {
    private final ConcurrentMap<String, LoyaltyAccount> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RedemptionVoucher> vouchers = new ConcurrentHashMap<>();

    @Override
    public LoyaltyAccount findOrCreate(String customerId) {
        return accounts.computeIfAbsent(customerId, LoyaltyAccount::new);
    }

    @Override
    public RedemptionVoucher save(RedemptionVoucher voucher) {
        vouchers.put(voucher.getCode(), voucher);
        return voucher;
    }

    @Override
    public Optional<RedemptionVoucher> findByCode(String code) {
        return Optional.ofNullable(vouchers.get(code));
    }
}