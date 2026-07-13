package com.example.freshfarm3.dto.response;

import com.example.freshfarm3.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FarmerOrderResponse — a farmer's view of an order they have items in.
 *
 * Deliberately NOT the same DTO as CheckoutResponse (the buyer's view):
 * this one carries grossEarnings / platformFee / netEarnings, which is
 * money information for the farmer only. Only items belonging to the
 * requesting farmer are included — an order can span multiple farmers,
 * and each farmer should only see their own items and their own earnings,
 * not the buyer's full bill or other farmers' items.
 */
@Data
@Builder
public class FarmerOrderResponse {

    private Long          orderId;
    private String        orderNumber;
    private OrderStatus   orderStatus;
    private String        paymentStatus;
    private LocalDateTime orderDate;
    private String        deliveryAddress;
    private List<OrderItemDto> items;

    // Money the farmer actually receives for their items in this order.
    private BigDecimal grossEarnings;   // sum of subtotal for the farmer's own items
    private BigDecimal platformFee;     // flat ₹5 per order — farmer-side only, buyers never see this
    private BigDecimal netEarnings;     // grossEarnings - platformFee

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
