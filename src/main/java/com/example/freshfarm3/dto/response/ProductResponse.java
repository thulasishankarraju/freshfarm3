package com.example.freshfarm3.dto.response;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean priceFixed;   // false = admin hasn't set a price yet; not purchasable
    private BigDecimal quantity;          // stock, in kg or liters
    private String unitType;              // "KG", "PIECE", "LITER"
    private BigDecimal avgPieceWeightGrams;
    private Integer approxPiecesAvailable; // only populated for PIECE products
    private String status;

    // Category info
    private Long categoryId;
    private String categoryName;

    // Shop info
    private Long shopId;
    private String ownerName;
    private String shopName;

    // Images
    private String primaryImageUrl;
    private List<String> imageUrls;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}