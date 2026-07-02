package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;        // FarmFresh internal order ID

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;         // in Rupees (backend converts to paise for Razorpay)
}

