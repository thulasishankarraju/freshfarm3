package com.example.freshfarm3.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Long buyerId;
    private String buyerName;
    private Long farmerId;
    private String farmerName;
    private Long orderId;
    private Integer rating;
    private String reviewTitle;
    private String reviewComment;
    private LocalDateTime reviewDate;
    private boolean verifiedPurchase;

    // ── Rating Summary (returned by /rating-summary endpoint) ──
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RatingSummary {
        private Double averageRating;
        private Long totalReviews;
        private Map<Integer, Long> ratingDistribution; // e.g. {5: 30, 4: 10, ...}
    }
}
