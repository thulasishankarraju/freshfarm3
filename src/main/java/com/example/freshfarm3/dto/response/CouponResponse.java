package com.example.freshfarm3.dto.response;

import com.example.freshfarm3.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {

    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumDiscount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer usageLimit;
    private Integer usedCount;
    private boolean active;
    private LocalDateTime createdAt;

    // ── Validation result (returned from /validate endpoint) ──
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationResult {
        private boolean valid;
        private String message;
        private BigDecimal discountAmount;   // Calculated discount in ₹
        private BigDecimal finalAmount;      // Order total after discount
        private String couponCode;
    }
}
