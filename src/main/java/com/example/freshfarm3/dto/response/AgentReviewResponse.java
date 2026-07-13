package com.example.freshfarm3.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentReviewResponse {

    private Long id;
    private Long agentId;
    private String agentName;
    private Long buyerId;
    private String buyerName;
    private Long orderId;
    private String orderNumber;
    private Integer rating;
    private String reviewComment;
    private LocalDateTime reviewDate;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RatingSummary {
        private Double averageRating;
        private Long totalReviews;
    }
}
