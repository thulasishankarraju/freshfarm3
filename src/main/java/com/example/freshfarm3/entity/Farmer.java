package com.example.freshfarm3.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "farmers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farmer extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "farm_name", nullable = false)
    private String farmName;

    @Column(name = "village")
    private String village;

    @Column(name = "district")
    private String district;

    @Column(name = "state")
    private String state;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Builder.Default
    @Column(name = "approved", nullable = false)
    private boolean approved = false;

    /**
     * Aggregate rating across all this farmer's products.
     * Recalculated by ReviewService.updateFarmerRating() whenever
     * a new review is created.
     */
    @Column(name = "average_rating", nullable = false)
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

}