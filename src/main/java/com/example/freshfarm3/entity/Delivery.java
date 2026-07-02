package com.example.freshfarm3.entity;

import com.example.freshfarm3.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_agent_id", nullable = false)
    private DeliveryAgent deliveryAgent;

    @Column(nullable = false, length = 6)
    private String otp;                    // 6-digit OTP for delivery confirmation

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    @Column
    private LocalDateTime pickedUpAt;

    @Column
    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus deliveryStatus;

    @Column
    private LocalDateTime estimatedDeliveryTime;

    @Column
    private Boolean otpVerified;
}