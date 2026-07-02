package com.example.freshfarm3.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentVerifyRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;               // FarmFresh internal order ID

    @NotBlank(message = "Razorpay Order ID is required")
    private String razorpayOrderId;     // rzp_order_xxxx

    @NotBlank(message = "Razorpay Payment ID is required")
    private String razorpayPaymentId;   // pay_xxxx

    @NotBlank(message = "Razorpay Signature is required")
    private String razorpaySignature;   // HMAC-SHA256 from Razorpay
}