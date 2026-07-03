package com.example.freshfarm3.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Farmer — Profile data for users with FARMER role.
 *
 * Maps to: farmers table in MySQL
 *
 * Sprint 1: Basic fields for registration.
 * Sprint 2+: Add product listings, earnings, approval status.
 */
@Entity
@Table(name = "farmers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farmer extends BaseEntity {

    /**
     * Each Farmer has exactly one User account (login credentials).
     * @OneToOne with cascade — if Farmer is deleted, User is NOT deleted
     *   (we use JoinColumn, not cascade delete)
     */
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

    /**
     * Aadhaar number — stored as string (12 digits + can have spaces/dashes)
     * In production: encrypt before storing.
     */
    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    /**
     * Admin must approve farmer before they can list products.
     * Default: false (pending approval)
     */
    @Builder.Default
    @Column(name = "approved", nullable = false)
    private boolean approved = false;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;
}