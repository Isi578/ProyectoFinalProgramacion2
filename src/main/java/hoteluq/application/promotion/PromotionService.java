package com.hoteluq.application.promotion;

import com.hoteluq.application.contract.PromotionAdministrationUseCase;
import com.hoteluq.application.port.PromotionPolicy;
import com.hoteluq.application.port.VoucherRepository;
import com.hoteluq.application.reservation.EntityNotFoundException;
import com.hoteluq.application.reservation.PromotionRejectedException;
import com.hoteluq.domain.loyalty.RedemptionVoucher;
import com.hoteluq.domain.model.Customer;
import com.hoteluq.domain.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces NoPromotionPolicy. The code a customer types is either a hotel promotion or a
 * single-use voucher obtained by redeeming points. Only one code applies per reservation,
 * and no discount can exceed 50% of the subtotal (rule BR-A05).
 */
public final class PromotionService implements PromotionPolicy, PromotionAdministrationUseCase {
    private final Map<String, Promotion> promotions = new ConcurrentHashMap<>();
    private final VoucherRepository vouchers;

    public PromotionService(VoucherRepository vouchers) { this.vouchers = vouchers; }

    public void registerPromotion(Promotion promotion) {
        if (promotions.putIfAbsent(promotion.getCode(), promotion) != null) {
            throw new IllegalArgumentException("Promotion already exists: " + promotion.getCode());
        }
    }

    @Override
    public void createPromotion(String code, BigDecimal discountRate,
                                LocalDateTime startsAt, LocalDateTime endsAt) {
        registerPromotion(Promotion.builder(code, new PercentageDiscount(discountRate))
                .validBetween(startsAt, endsAt)
                .build());
    }

    @Override
    public void deactivatePromotion(String code) {
        Promotion promotion = promotions.get(normalize(code));
        if (promotion == null) throw new EntityNotFoundException("Promotion not found: " + code);
        promotion.deactivate();
    }

    public List<Promotion> listPromotions() {
        return promotions.values().stream()
                .sorted((a, b) -> a.getCode().compareTo(b.getCode())).toList();
    }

    @Override
    public synchronized BigDecimal apply(String promotionCode, Customer customer, Room room,
                                         LocalDateTime checkInAt, LocalDateTime checkOutAt,
                                         BigDecimal subtotal) {
        if (promotionCode == null || promotionCode.isBlank()) return subtotal;
        String code = normalize(promotionCode);

        Promotion promotion = promotions.get(code);
        if (promotion != null) {
            return applyPromotion(promotion, customer, room, checkInAt, checkOutAt, subtotal);
        }
        RedemptionVoucher voucher = vouchers.findByCode(code).orElse(null);
        if (voucher != null) return applyVoucher(voucher, customer, subtotal);

        throw new PromotionRejectedException("Unknown promotion code: " + promotionCode.trim());
    }

    private BigDecimal applyPromotion(Promotion promotion, Customer customer, Room room,
                                      LocalDateTime checkInAt, LocalDateTime checkOutAt,
                                      BigDecimal subtotal) {
        if (!promotion.isActive()) {
            throw new PromotionRejectedException("Promotion " + promotion.getCode() + " is not active.");
        }
        long nights = ChronoUnit.DAYS.between(checkInAt.toLocalDate(), checkOutAt.toLocalDate());
        PromotionContext context = new PromotionContext(
                customer, room, checkInAt, checkOutAt, nights, subtotal);
        BigDecimal discount = promotion.discountFor(context).min(maxDiscount(subtotal));
        return subtotal.subtract(discount);
    }

    private BigDecimal applyVoucher(RedemptionVoucher voucher, Customer customer, BigDecimal subtotal) {
        if (!voucher.getCustomerId().equals(customer.getId())) {
            throw new PromotionRejectedException("This voucher belongs to another customer.");
        }
        if (voucher.isUsed()) {
            throw new PromotionRejectedException("This voucher was already used.");
        }
        if (voucher.getValue().compareTo(maxDiscount(subtotal)) > 0) {
            throw new PromotionRejectedException("The voucher (" + voucher.getValue().toPlainString()
                    + " COP) exceeds 50% of the reservation subtotal; it was not consumed.");
        }
        voucher.markUsed();
        return subtotal.subtract(voucher.getValue());
    }

    private static BigDecimal maxDiscount(BigDecimal subtotal) {
        return subtotal.multiply(DiscountStrategy.MAX_DISCOUNT_RATE).setScale(0, RoundingMode.DOWN);
    }

    private static String normalize(String code) { return code.trim().toUpperCase(); }
}