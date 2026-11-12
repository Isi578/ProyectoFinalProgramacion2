package com.hoteluq.application.reservation;

import java.time.LocalDateTime;
import java.util.List;

public final class ReservationRequest {
    private final String customerId;
    private final String roomNumber;
    private final LocalDateTime checkInAt;
    private final LocalDateTime checkOutAt;
    private final int guestCount;
    private final List<String> serviceIds;
    private final String cardId;
    private final String promotionCode;

    private ReservationRequest(Builder builder) {
        customerId = builder.customerId;
        roomNumber = builder.roomNumber;
        checkInAt = builder.checkInAt;
        checkOutAt = builder.checkOutAt;
        guestCount = builder.guestCount;
        serviceIds = List.copyOf(builder.serviceIds);
        cardId = builder.cardId;
        promotionCode = builder.promotionCode;
    }

    public static Builder builder() { return new Builder(); }
    public String getCustomerId() { return customerId; }
    public String getRoomNumber() { return roomNumber; }
    public LocalDateTime getCheckInAt() { return checkInAt; }
    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public int getGuestCount() { return guestCount; }
    public List<String> getServiceIds() { return serviceIds; }
    public String getCardId() { return cardId; }
    public String getPromotionCode() { return promotionCode; }

    public static final class Builder {
        private String customerId;
        private String roomNumber;
        private LocalDateTime checkInAt;
        private LocalDateTime checkOutAt;
        private int guestCount = 1;
        private List<String> serviceIds = List.of();
        private String cardId;
        private String promotionCode;

        public Builder customerId(String value) { customerId = value; return this; }
        public Builder roomNumber(String value) { roomNumber = value; return this; }
        public Builder checkInAt(LocalDateTime value) { checkInAt = value; return this; }
        public Builder checkOutAt(LocalDateTime value) { checkOutAt = value; return this; }
        public Builder guestCount(int value) { guestCount = value; return this; }
        public Builder serviceIds(List<String> value) { serviceIds = value == null ? List.of() : List.copyOf(value); return this; }
        public Builder cardId(String value) { cardId = value; return this; }
        public Builder promotionCode(String value) { promotionCode = value; return this; }

        public ReservationRequest build() {
            return new ReservationRequest(this);
        }
    }
}