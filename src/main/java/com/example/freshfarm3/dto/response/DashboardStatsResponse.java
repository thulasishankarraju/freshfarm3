package com.example.freshfarm3.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    // ── User stats ───────────────────────────────────────────────────────────
    private long totalUsers;
    private long totalBuyers;
    private long totalShops;
    private long pendingShops;
    private long approvedShops;

    // ── Product stats ────────────────────────────────────────────────────────
    private long totalProducts;
    private long activeProducts;
    private double averageProductRating;

    // ── Order stats ──────────────────────────────────────────────────────────
    private long ordersToday;
    private long ordersThisMonth;
    private long totalOrders;
    private long deliveredOrders;
    private long pendingDeliveries;

    // ── Revenue ──────────────────────────────────────────────────────────────
    private BigDecimal totalRevenue;
    private BigDecimal revenueToday;
    private BigDecimal revenueThisMonth;

    // ── Delivery agents ──────────────────────────────────────────────────────
    private long activeDeliveryAgents;

    // ── Review stats ─────────────────────────────────────────────────────────
    private long totalReviews;

    // ── Subscription stats ───────────────────────────────────────────────────
    private long activeSubscriptions;

    // ── Charts data (for frontend) ───────────────────────────────────────────
    private List<Map<String, Object>> revenueByMonth;   // [{month:"Jan", revenue:5000}, ...]
    private List<Map<String, Object>> ordersByMonth;    // [{month:"Jan", orders:120}, ...]
    private List<Map<String, Object>> topProducts;      // [{name, sales, revenue}, ...]
    private List<Map<String, Object>> topShops;       // [{name, rating, totalSales}, ...]

    // ── Shop approval shortlist ─────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ShopSummary {
        private Long   shopId;
        private String ownerName;
        private String email;
        private String phone;
        private String shopName;
        private String shopLocation;
        private String approvalStatus;  // PENDING / APPROVED / REJECTED
        private String registeredDate;
    }
}
