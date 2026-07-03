package com.example.freshfarm3.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * FarmerResponse — Full profile view of a Farmer, returned from
 * "my profile" / farmer-detail endpoints.
 *
 * (For the trimmed-down list admins see while approving pending farmers,
 * see {@link DashboardStatsResponse.FarmerSummary} instead — this one
 * carries the fuller set of fields a farmer needs to see about themself.)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerResponse {

    private Long farmerId;

    // ── Account info (from the linked User) ─────────────────────
    private String fullName;
    private String email;
    private String phone;

    // ── Farm profile ─────────────────────────────────────────────
    private String farmName;
    private String village;
    private String district;
    private String state;
    private String pincode;
    private String farmLocation;   // village + district + state, combined
    private String bio;

    // ── Approval workflow ────────────────────────────────────────
    private boolean approved;
    private String approvalStatus; // PENDING / APPROVED / REJECTED

    // ── Stats ───────────────────────────────────────────────────
    private Double averageRating;
    private Long totalProducts;

    private LocalDateTime memberSince;
}
