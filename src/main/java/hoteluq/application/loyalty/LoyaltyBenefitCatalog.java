package hoteluq.application.loyalty;

import com.hoteluq.domain.loyalty.BenefitType;
import com.hoteluq.domain.loyalty.LoyaltyBenefit;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Point equivalences. Default group proposal: 1 point = COP 20 of credit
 * (so earning 100 points per COP 100,000 returns about 2%).
 */
public final class LoyaltyBenefitCatalog {
    private final Map<String, LoyaltyBenefit> benefits = new LinkedHashMap<>();

    public LoyaltyBenefitCatalog(List<LoyaltyBenefit> initialBenefits) {
        initialBenefits.forEach(benefit -> benefits.put(benefit.id(), benefit));
    }

    public static LoyaltyBenefitCatalog defaults() {
        return new LoyaltyBenefitCatalog(List.of(
                new LoyaltyBenefit("DISC_20K", "COP 20,000 off a reservation",
                        BenefitType.RESERVATION_DISCOUNT, 1000, new BigDecimal("20000")),
                new LoyaltyBenefit("SVC_SPA", "Spa access",
                        BenefitType.SERVICE, 3000, new BigDecimal("60000")),
                new LoyaltyBenefit("PKG_WELLNESS", "Wellness package",
                        BenefitType.SERVICE_PACKAGE, 5000, new BigDecimal("100000"))));
    }

    public Optional<LoyaltyBenefit> findById(String benefitId) {
        return Optional.ofNullable(benefits.get(benefitId));
    }

    public List<LoyaltyBenefit> findAll() { return List.copyOf(benefits.values()); }
}