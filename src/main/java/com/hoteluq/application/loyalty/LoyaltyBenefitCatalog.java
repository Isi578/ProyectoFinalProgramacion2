package com.hoteluq.application.loyalty;

import com.hoteluq.domain.loyalty.BenefitType;
import com.hoteluq.domain.loyalty.LoyaltyBenefit;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Point equivalences. Default group proposal: 1 point = COP 50 of credit
 * (so earning 1 point per COP 500 paid returns about 10%).
 */
public final class LoyaltyBenefitCatalog {
    private final Map<String, LoyaltyBenefit> benefits = new LinkedHashMap<>();

    public LoyaltyBenefitCatalog(List<LoyaltyBenefit> initialBenefits) {
        initialBenefits.forEach(benefit -> benefits.put(benefit.id(), benefit));
    }

    public static LoyaltyBenefitCatalog defaults() {
        return new LoyaltyBenefitCatalog(List.of(
                new LoyaltyBenefit("DISC_20K", "Descuento de 20.000 COP en una reserva",
                        BenefitType.RESERVATION_DISCOUNT, 400, new BigDecimal("20000")),
                new LoyaltyBenefit("SVC_SPA", "Acceso al spa",
                        BenefitType.SERVICE, 1200, new BigDecimal("60000")),
                new LoyaltyBenefit("PKG_WELLNESS", "Paquete bienestar",
                        BenefitType.SERVICE_PACKAGE, 2000, new BigDecimal("100000"))));
    }

    /** Registra beneficios leídos del archivo opcional de demostración. */
    public synchronized void add(LoyaltyBenefit benefit) {
        if (benefits.containsKey(benefit.id())) {
            throw new IllegalArgumentException("El beneficio ya existe: " + benefit.id());
        }
        benefits.put(benefit.id(), benefit);
    }

    public Optional<LoyaltyBenefit> findById(String benefitId) {
        return Optional.ofNullable(benefits.get(benefitId));
    }

    public List<LoyaltyBenefit> findAll() { return List.copyOf(benefits.values()); }
}