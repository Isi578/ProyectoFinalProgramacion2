package com.hoteluq.domain.loyalty;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SpendBlockAccrualStrategy implements PointsAccrualStrategy {
    private final BigDecimal blockSize;
    private final int pointsPerBlock;

    public SpendBlockAccrualStrategy() { this(new BigDecimal("100000"), 100); }

    public SpendBlockAccrualStrategy(BigDecimal blockSize, int pointsPerBlock) {
        if (blockSize == null || blockSize.signum() <= 0) {
            throw new LoyaltyException("Block size must be positive.");
        }
        if (pointsPerBlock < 1) throw new LoyaltyException("Points per block must be positive.");
        this.blockSize = blockSize;
        this.pointsPerBlock = pointsPerBlock;
    }

    @Override
    public int pointsFor(BigDecimal amountPaid) {
        if (amountPaid == null || amountPaid.signum() <= 0) return 0;
        long blocks = amountPaid.divideToIntegralValue(blockSize)
                .setScale(0, RoundingMode.DOWN).longValueExact();
        return Math.toIntExact(blocks * pointsPerBlock);
    }
}