package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * ProductPriceRequest — Payload for PUT /api/admin/products/{id}/price.
 * Only an admin may fix (or later change) a product's selling price.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPriceRequest {

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    private BigDecimal price;
}
