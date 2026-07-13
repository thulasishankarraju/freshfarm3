package com.example.freshfarm3.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "delivery_agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAgent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 20)
    private String vehicleNumber;

    @Column(nullable = false, length = 30)
    private String vehicleType;        // "BIKE", "VAN", "CYCLE"

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(precision = 10, scale = 7)
    private BigDecimal currentLatitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal currentLongitude;

    // ── Reviews: aggregate rating fields (buyers rate the agent after
    // delivery, 1–5 stars) ─────────────────────────────────────────
    @Column
    @Builder.Default
    private Double averageRating = 0.0;

    @Column
    @Builder.Default
    private Integer reviewCount = 0;
}