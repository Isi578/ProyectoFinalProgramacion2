package com.hoteluq.infrastructure.factory;

import com.hoteluq.application.loyalty.LoyaltyPointsObserver;
import com.hoteluq.application.loyalty.LoyaltyService;
import com.hoteluq.application.promotion.PromotionService;
import com.hoteluq.application.service.ServiceAdministrationService;

/** Services, promotions and points objects, already wired together. */
public final class LoyaltyPromotionComponents {
    private final LoyaltyService loyaltyService;
    private final LoyaltyPointsObserver loyaltyObserver;
    private final PromotionService promotionService;
    private final ServiceAdministrationService serviceAdministration;

    public LoyaltyPromotionComponents(LoyaltyService loyaltyService,
                                      LoyaltyPointsObserver loyaltyObserver,
                                      PromotionService promotionService,
                                      ServiceAdministrationService serviceAdministration) {
        this.loyaltyService = loyaltyService;
        this.loyaltyObserver = loyaltyObserver;
        this.promotionService = promotionService;
        this.serviceAdministration = serviceAdministration;
    }

    public LoyaltyService getLoyaltyService() { return loyaltyService; }
    public LoyaltyPointsObserver getLoyaltyObserver() { return loyaltyObserver; }
    public PromotionService getPromotionService() { return promotionService; }
    public ServiceAdministrationService getServiceAdministration() { return serviceAdministration; }
}