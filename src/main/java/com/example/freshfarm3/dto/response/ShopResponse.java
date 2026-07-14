package com.example.freshfarm3.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * ShopResponse — Full profile view of a Shop, returned from
 * "my profile" / shop-detail endpoints.
 *
 * (For the trimmed-down list admins see while approving pending shops,
 * see {@link DashboardStatsResponse.ShopSummary} instead — this one
 * carries the fuller set of fields a shop needs to see about themself.)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopResponse {

    private Long shopId;

    // ── Account info (from the linked User) ─────────────────────
    private String fullName;
    private String email;
    private String phone;

    // ── Farm profile ─────────────────────────────────────────────
    private String shopName;
    private String village;
    private String district;
    private String state;
    private String pincode;
    private String shopLocation;   // village + district + state, combined
    private String bio;

    // ── Approval workflow ────────────────────────────────────────
    private boolean approved;
    private String approvalStatus; // PENDING / APPROVED / REJECTED

    // ── Stats ───────────────────────────────────────────────────
    private Double averageRating;
    private Long totalProducts;

    private LocalDateTime memberSince;
}
