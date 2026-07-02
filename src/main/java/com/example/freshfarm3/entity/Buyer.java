package com.example.freshfarm3.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Buyer — Profile data for users with BUYER role.
 *
 * Maps to: buyers table in MySQL
 *
 * Sprint 1: Basic profile created during registration.
 * Sprint 2+: Add addresses, order history, cart.
 */
@Entity
@Table(name = "buyers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Buyer extends BaseEntity {

    /**
     * Each Buyer has exactly one User account (login credentials).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    /**
     * Preferred delivery address (set after first order).
     * Sprint 2: Link to Address entity.
     */
    @Column(name = "default_address")
    private String defaultAddress;
}