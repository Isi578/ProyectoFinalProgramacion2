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

        seedPoints(repository);
        seedPromotions(promotions, hotel);

        return new LoyaltyPromotionComponents(loyalty, new LoyaltyPointsObserver(loyalty),
                promotions, new ServiceAdministrationService(serviceStore));
    }

    private static void seedPoints(InMemoryLoyaltyRepository repository) {
        var account = repository.findOrCreate(DEMO_CUSTOMER_ID);
        account.accrue("SEED-OLD", 200, LocalDateTime.of(2025, 10, 1, 12, 0));   // already expired
        account.accrue("SEED-1", 1000, LocalDateTime.of(2026, 1, 1, 12, 0));
        account.accrue("SEED-2", 400, LocalDateTime.of(2026, 7, 16, 12, 0));
        account.accrue("SEED-3", 1200, LocalDateTime.of(2026, 10, 25, 12, 0));
    }

    private static void seedPromotions(PromotionService promotions, HotelComponents hotel) {
        promotions.registerPromotion(Promotion.builder("VERANO10", new PercentageDiscount(new BigDecimal("0.10")))
                .name("Summer: 10% off stays of 2+ nights")
                .validBetween(LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2027, 3, 31, 23, 59))
                .minNights(2)
                .build());
        promotions.registerPromotion(Promotion.builder("SUITE15", new PercentageDiscount(new BigDecimal("0.15")))
                .name("15% off suites")
                .forRoomTypes("SUITE")
                .build());
        promotions.registerPromotion(Promotion.builder("FRECUENTE5", new PercentageDiscount(new BigDecimal("0.05")))
                .name("5% off for returning guests")
                .frequentCustomer(hotel.getReservationRepository(), 1)
                .build());
    }
}