package com.example.freshfarm3.entity;

import com.example.freshfarm3.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ShopPayout — Represents an amount the admin has fixed as owed to a
 * shop for the products they sold, and tracks whether that amount has
 * actually been sent to the shop's bank account yet.
 *
 * Flow:
 *   1. Admin reviews a shop's sales and creates a ShopPayout with the
 *      amount to be paid (status = PENDING).
 *   2. The shop sees this amount (and its PENDING/PAID status) on their
 *      dashboard.
 *   3. Once the admin actually transfers the money (outside this system,
 *      e.g. by bank transfer to the account details captured at shop
 *      registration), the admin marks the payout PAID.
 */
@Entity
@Table(name = "shop_payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopPayout extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(length = 500)
    private String note;

    @Column
    private LocalDateTime paidAt;
}
