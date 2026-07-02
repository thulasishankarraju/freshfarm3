package com.example.freshfarm3.entity;

import com.example.freshfarm3.enums.PaymentMethod;
import com.example.freshfarm3.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // ── Razorpay fields ──────────────────────────────────────────
    @Column(length = 60)
    private String razorpayOrderId;      // rzp_order_xxxx

    @Column(length = 60)
    private String razorpayPaymentId;    // pay_xxxx (set after payment)

    @Column(length = 256)
    private String razorpaySignature;    // HMAC-SHA256 signature

    // ── Payment details ──────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column
    private LocalDateTime paymentDate;

    @Column(length = 100)
    private String transactionReference;

    @Column(length = 30)
    @Builder.Default
    private String gateway = "RAZORPAY";

    // ── Refund ───────────────────────────────────────────────────
    @Column(length = 60)
    private String refundId;             // rfnd_xxxx (set after refund)
}
