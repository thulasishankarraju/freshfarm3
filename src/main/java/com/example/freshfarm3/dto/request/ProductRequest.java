package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

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
    private BigDecimal quantity;   // stock, always in kg (KG/PIECE products) or liters (LITER products)

    @NotBlank(message = "Unit type is required")
    private String unitType;       // "KG", "PIECE", or "LITER"

    // Required only when unitType = PIECE — average weight of one piece,
    // used to convert piece-count orders into a kg stock deduction.
    @DecimalMin(value = "0.01", message = "Average piece weight must be greater than 0")
    private BigDecimal avgPieceWeightGrams;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // Optional — if not provided defaults to ACTIVE
    private String status;
}