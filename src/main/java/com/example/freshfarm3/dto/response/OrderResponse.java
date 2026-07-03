package com.example.freshfarm3.dto.response;

import com.example.freshfarm3.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Flat, read-only view of an Order for admin / buyer order listings.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private OrderStatus orderStatus;
    private String paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal deliveryCharge;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    private Long buyerId;
    private String buyerName;
}
