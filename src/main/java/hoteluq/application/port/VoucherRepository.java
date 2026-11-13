package com.hoteluq.application.port;

import com.hoteluq.domain.loyalty.RedemptionVoucher;

import java.util.Optional;

public interface VoucherRepository {
    RedemptionVoucher save(RedemptionVoucher voucher);
    Optional<RedemptionVoucher> findByCode(String code);
}