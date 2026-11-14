package com.hoteluq.application.promotion;

import com.hoteluq.application.port.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A promotion: a code, a discount strategy and a list of eligibility conditions. */
public final class Promotion {
    private final String code;
    private final String name;
    private final DiscountStrategy discount;
    private final List<PromotionCondition> conditions;
    private boolean active = true;

    private Promotion(Builder builder) {
        this.code = builder.code.trim().toUpperCase();
        this.name = builder.name == null || builder.name.isBlank() ? this.code : builder.name.trim();
        this.discount = builder.discount;
        this.conditions = List.copyOf(builder.conditions);
    }

    public static Builder builder(String code, DiscountStrategy discount) {
        return new Builder(code, discount);
    }

    /** Checks every condition in order, then returns the discount for the subtotal. */
    public BigDecimal discountFor(PromotionContext context) {
        for (PromotionCondition condition : conditions) {
            condition.check(context);
        }
        return discount.discountFor(context.subtotal());
    }

    public void deactivate() { active = false; }
    public boolean isActive() { return active; }
    public String getCode() { return code; }
    public String getName() { return name; }

    /** Builder: adds conditions one by one and creates the promotion at the end. */
    public static final class Builder {
        private final String code;
        private final DiscountStrategy discount;
        private String name;
        private final List<PromotionCondition> conditions = new ArrayList<>();

        private Builder(String code, DiscountStrategy discount) {
            if (code == null || code.isBlank()) throw new IllegalArgumentException("Promotion code is required.");
            if (discount == null) throw new IllegalArgumentException("Discount strategy is required.");
            this.code = code;
            this.discount = discount;
        }

        public Builder name(String value) { name = value; return this; }

        public Builder validBetween(LocalDateTime startsAt, LocalDateTime endsAt) {
            conditions.add(new DateRangeCondition(startsAt, endsAt));
            return this;
        }

        public Builder forRoomTypes(String... roomTypeIds) {
            conditions.add(new RoomTypeCondition(Set.of(roomTypeIds)));
            return this;
        }

        public Builder minNights(int nights) {
            conditions.add(new MinNightsCondition(nights));
            return this;
        }

        public Builder frequentCustomer(ReservationRepository reservations, int minReservations) {
            conditions.add(new FrequentCustomerCondition(reservations, minReservations));
            return this;
        }

        public Promotion build() { return new Promotion(this); }
    }
}