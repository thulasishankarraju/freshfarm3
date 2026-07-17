package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * PayoutRequest — Payload for POST /api/admin/shops/{shopId}/payouts.
 * Admin fixes the amount owed to a shop for their product sales.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String note;
}
