package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    // Buyer-facing quantity: kg/liters (0.25 steps) or whole pieces,
    // depending on the product's unitType. Not an Integer — a PIECE
    // product still needs whole numbers, but KG/LITER products need
    // fractional amounts like 0.25 or 1.5.
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;
}