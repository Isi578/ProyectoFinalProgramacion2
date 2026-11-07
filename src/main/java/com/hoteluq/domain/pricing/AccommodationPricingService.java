package com.hoteluq.domain.pricing;

import com.hoteluq.domain.model.Room;
import com.hoteluq.domain.model.ServiceOption;

import java.util.List;

public final class AccommodationPricingService {
    public java.math.BigDecimal calculate(Room room, long nights, int guestCount,
                                          List<ServiceOption> services) {
        PriceComponent price = new RoomStayPrice(room, nights);
        price = new ServicePriceDecorator(price, services, nights, guestCount);
        return price.amount();
    }
}