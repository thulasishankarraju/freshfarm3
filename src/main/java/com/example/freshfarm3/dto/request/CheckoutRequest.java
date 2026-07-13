package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotNull(message = "Delivery address ID is required")
    private Long addressId;

    // Optional: coupon code (Sprint 5 feature, accepted here but not processed)
    private String couponCode;

    // Payment method — defaults to COD in Sprint 3; Razorpay added in Sprint 4
    private String paymentMethod = "COD";

    /**
     * How the buyer wants to receive the delivery OTP: "EMAIL", "PHONE", or "BOTH".
     * Optional — defaults to "BOTH" on the backend if not supplied.
     */
    private String otpChannel;

    /**
     * Optional tip for the delivery agent. Buyer picks a fixed amount
     * (₹10/₹20/₹30) or enters a custom amount on the checkout page.
     * Goes to the agent in full — the platform takes no cut of this.
     * Defaults to ₹0 if not supplied.
     */
    private java.math.BigDecimal tipAmount;
}