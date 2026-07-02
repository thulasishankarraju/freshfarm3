package com.example.freshfarm3.dto.request;

import com.example.freshfarm3.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50, message = "Code must be between 3 and 50 characters")
    private String code;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @NotNull(message = "Minimum order amount is required")
    @DecimalMin(value = "0.00", message = "Minimum order amount must be 0 or more")
    private BigDecimal minimumOrderAmount;

    // Optional — null means no cap
    @DecimalMin(value = "0.01", message = "Maximum discount must be greater than 0")
    private BigDecimal maximumDiscount;

    @NotNull(message = "Valid from date is required")
    private LocalDateTime validFrom;

    @NotNull(message = "Valid until date is required")
    private LocalDateTime validUntil;

    // 0 = unlimited
    @NotNull(message = "Usage limit is required")
    @Min(value = 0, message = "Usage limit cannot be negative")
    private Integer usageLimit;

    private boolean active = true;
}
