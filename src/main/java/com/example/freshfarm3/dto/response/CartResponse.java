package com.example.freshfarm3.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {

    private Long cartId;
    private Long buyerId;
    private List<CartItemResponseDto> items;
    private BigDecimal totalAmount;
    private int totalItems;

    @Data
    @Builder
    public static class CartItemResponseDto {
        private Long cartItemId;
        private Long productId;
        private String productName;
        private String productUnit;
        private String imageUrl;
        private BigDecimal pricePerUnit;
        private Integer quantity;
        private BigDecimal subtotal;
        private Integer availableStock;
        private Boolean isAvailable;
    }
}
