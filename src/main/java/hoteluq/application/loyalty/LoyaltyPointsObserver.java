package hoteluq.application.loyalty;

import com.hoteluq.application.reservation.ReservationEvent;
import com.hoteluq.application.reservation.ReservationEventObserver;
import com.hoteluq.domain.model.Reservation;

public final class LoyaltyPointsObserver implements ReservationEventObserver {
    private final LoyaltyService loyaltyService;

    public LoyaltyPointsObserver(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        Reservation reservation = event.getReservation();
        String customerId = reservation.getCustomer().getId();
        switch (event.getType()) {
            case CREATED -> loyaltyService.recordEligibleSpend(
                    customerId, reservation.getId(), reservation.getTotalPaid());
            case CANCELLED -> loyaltyService.revertSpend(customerId, reservation.getId());
        }
    }
}