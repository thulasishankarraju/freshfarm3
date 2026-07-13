package com.example.freshfarm3.dto.response;

import com.example.freshfarm3.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CheckoutResponse {

    private Long          orderId;
    private String        orderNumber;
    private OrderStatus   orderStatus;
    private String        paymentStatus;
    private BigDecimal    subtotal;
    private BigDecimal    deliveryCharge;
    private BigDecimal    platformFee;
    private BigDecimal    tipAmount;
    private BigDecimal    totalAmount;
    private LocalDateTime orderDate;
    private String        deliveryAddress;
    private List<OrderItemDto> items;

    @Data
    @Builder
    public static class OrderItemDto {
        private Long       productId;
        private String     productName;
        private BigDecimal quantity;
        private String     unitType;
        private BigDecimal pricePerUnit;
        private BigDecimal subtotal;
    }
}