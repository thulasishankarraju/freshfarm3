package com.example.freshfarm3.dto.response;

import com.example.freshfarm3.enums.PaymentMethod;
import com.example.freshfarm3.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private Long          paymentId;
    private Long          orderId;
    private String        orderNumber;
    private String        razorpayOrderId;
    private String        razorpayPaymentId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal    amount;
    private String        currency;
    private LocalDateTime paymentDate;
    private String        transactionReference;
    private String        gateway;
    private String        refundId;
}
