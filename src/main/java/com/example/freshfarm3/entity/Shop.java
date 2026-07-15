package com.example.freshfarm3.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "shop_name", nullable = false)
    private String shopName;

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

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    /**
     * Average rating received across all products of this shop.
     */
    @Builder.Default
    @Column(name = "average_rating", nullable = false)
    private Double averageRating = 0.0;
}