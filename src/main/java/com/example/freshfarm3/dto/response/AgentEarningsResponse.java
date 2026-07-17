package com.example.freshfarm3.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentEarningsResponse {

    // Summary
    private long todayDeliveries;
    private BigDecimal todayEarnings;
    private long totalDeliveries;
    private BigDecimal totalEarnings;
    private BigDecimal totalBonus;
    private int deliveriesUntilNextBonus; // how many more deliveries today until the next ₹9 bonus

    // Absorbed from an earlier duplicate /api/delivery/earnings endpoint —
    // kept here so nothing is lost now that there's a single canonical
    // earnings response.
    private long pendingDeliveries;    // assigned/picked-up/out-for-delivery, not yet completed
    private Double averageRating;      // agent's buyer rating (see AgentReviewService)
    private Integer reviewCount;
    private Boolean isAvailable;

    private List<EarningItem> recent;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EarningItem {
        private Long id;
        private String type;      // DELIVERY_FEE / BONUS
        private BigDecimal amount;
        private String orderNumber; // null for pure bonus rows not tied to a single order
        private String note;
        private LocalDateTime createdAt;
    }
}
