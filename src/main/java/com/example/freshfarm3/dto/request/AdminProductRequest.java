package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * AdminProductRequest — Payload for POST /api/admin/products.
 *
 * Unlike ProductRequest (used by shops), price IS required here and is
 * applied immediately (priceFixed = true) — the admin is both adding the
 * product and fixing its price in one step. The admin also chooses which
 * shop the product is listed under.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductRequest {

    @NotNull(message = "Shop ID is required")
    private Long shopId;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Quantity cannot be negative")
    private BigDecimal quantity;

    @NotBlank(message = "Unit type is required")
    private String unitType;

    @DecimalMin(value = "0.01", message = "Average piece weight must be greater than 0")
    private BigDecimal avgPieceWeightGrams;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private String status;
}

