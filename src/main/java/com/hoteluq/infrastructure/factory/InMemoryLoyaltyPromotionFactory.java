package com.hoteluq.infrastructure.factory;

import com.hoteluq.application.loyalty.LoyaltyBenefitCatalog;
import com.hoteluq.application.loyalty.LoyaltyPointsObserver;
import com.hoteluq.application.loyalty.LoyaltyService;
import com.hoteluq.application.port.ServiceStore;
import com.hoteluq.application.promotion.PercentageDiscount;
import com.hoteluq.application.promotion.Promotion;
import com.hoteluq.application.promotion.PromotionService;
import com.hoteluq.application.service.ServiceAdministrationService;
import com.hoteluq.domain.loyalty.SpendBlockAccrualStrategy;
import com.hoteluq.infrastructure.memory.InMemoryLoyaltyRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/** Builds the services/promotions/points module with demo data for the guest GUEST-ANA-001. */
public final class InMemoryLoyaltyPromotionFactory {
    public static final String DEMO_CUSTOMER_ID = "GUEST-ANA-001";

    public LoyaltyPromotionComponents create(HotelComponents hotel, Clock clock) {
        if (!(hotel.getServiceCatalog() instanceof ServiceStore serviceStore)) {
            throw new IllegalStateException("The service catalog must support writes (ServiceStore).");
        }

        InMemoryLoyaltyRepository repository = new InMemoryLoyaltyRepository();
        LoyaltyService loyalty = new LoyaltyService(repository, repository,
                new SpendBlockAccrualStrategy(), LoyaltyBenefitCatalog.defaults(), clock);
        PromotionService promotions = new PromotionService(repository);

        // No hay puntos ni promociones precargados automáticamente.
        // Las cuentas empiezan vacías hasta realizar compras o reservas.


        return new LoyaltyPromotionComponents(loyalty, new LoyaltyPointsObserver(loyalty),
                promotions, new ServiceAdministrationService(serviceStore));
    }


}