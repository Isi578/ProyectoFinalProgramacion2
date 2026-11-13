package hoteluq.application.loyalty;

import com.hoteluq.application.contract.LoyaltyPointsUseCase;
import com.hoteluq.application.port.LoyaltyAccountRepository;
import com.hoteluq.application.port.LoyaltyPointsPort;
import com.hoteluq.application.port.VoucherRepository;
import com.hoteluq.domain.loyalty.LoyaltyAccount;
import com.hoteluq.domain.loyalty.LoyaltyBenefit;
import com.hoteluq.domain.loyalty.LoyaltyEntry;
import com.hoteluq.domain.loyalty.LoyaltyException;
import com.hoteluq.domain.loyalty.PointsAccrualStrategy;
import com.hoteluq.domain.loyalty.RedemptionVoucher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Points ledger: accrual, reversal on cancellation, redemption and history. */
public final class LoyaltyService implements LoyaltyPointsPort, LoyaltyPointsUseCase {
    private final LoyaltyAccountRepository accounts;
    private final VoucherRepository vouchers;
    private final PointsAccrualStrategy accrualStrategy;
    private final LoyaltyBenefitCatalog benefitCatalog;
    private final Clock clock;

    public LoyaltyService(LoyaltyAccountRepository accounts, VoucherRepository vouchers,
                          PointsAccrualStrategy accrualStrategy,
                          LoyaltyBenefitCatalog benefitCatalog, Clock clock) {
        this.accounts = accounts;
        this.vouchers = vouchers;
        this.accrualStrategy = accrualStrategy;
        this.benefitCatalog = benefitCatalog;
        this.clock = clock;
    }

    @Override
    public synchronized void recordEligibleSpend(String customerId, String reservationId,
                                                 BigDecimal amountPaid) {
        int points = accrualStrategy.pointsFor(amountPaid);
        if (points < 1) return;
        accounts.findOrCreate(customerId).accrue(reservationId, points, now());
    }

    /** Called when a reservation is cancelled: the points it generated no longer apply. */
    public synchronized int revertSpend(String customerId, String reservationId) {
        return accounts.findOrCreate(customerId).revoke(reservationId, now());
    }

    @Override
    public synchronized int getAvailablePoints(String customerId) {
        return accounts.findOrCreate(customerId).availablePoints(now());
    }

    /** Contract method: {@code points} must match the benefit's cost exactly. */
    @Override
    public synchronized void redeemPoints(String customerId, int points, String benefitId) {
        LoyaltyBenefit benefit = requireBenefit(benefitId);
        if (points != benefit.pointsCost()) {
            throw new LoyaltyException("Benefit " + benefitId + " costs "
                    + benefit.pointsCost() + " points, not " + points + ".");
        }
        redeemBenefit(customerId, benefitId);
    }

    /** Spends the points of a benefit and returns the single-use voucher code for it. */
    public synchronized RedemptionVoucher redeemBenefit(String customerId, String benefitId) {
        LoyaltyBenefit benefit = requireBenefit(benefitId);
        LoyaltyAccount account = accounts.findOrCreate(customerId);
        account.redeem(benefit.pointsCost(), "benefit " + benefit.id(), now());

        String code = "PTS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return vouchers.save(new RedemptionVoucher(
                code, account.getCustomerId(), benefit.id(), benefit.value(), now()));
    }

    @Override
    public synchronized List<String> getPointHistory(String customerId) {
        return accounts.findOrCreate(customerId).getHistory().stream()
                .map(LoyaltyEntry::toString).toList();
    }

    public List<LoyaltyBenefit> listBenefits() { return benefitCatalog.findAll(); }

    private LoyaltyBenefit requireBenefit(String benefitId) {
        return benefitCatalog.findById(benefitId)
                .orElseThrow(() -> new LoyaltyException("Unknown benefit: " + benefitId));
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }
}