package com.example.freshfarm3.entity;

import com.example.freshfarm3.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_address_id", nullable = false)
    private Address deliveryAddress;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryCharge;

    // Flat ₹5 platform fee charged to the buyer on every order. Shown to
    // the buyer as its own line item — this is separate from (and does
    // NOT affect) the shop-side platform fee, which is deducted from
    // shop earnings and is never exposed on the buyer's order data.
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    // Optional tip for the delivery agent — buyer picks a fixed amount
    // (₹10/₹20/₹30) or enters a custom amount at checkout. Goes to the
    // agent in full; the platform does not take a cut of this.
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tipAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 30)
    private String paymentStatus;   // PENDING / PAID / FAILED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    /**
     * Buyer's preferred channel for receiving the delivery OTP: "EMAIL", "PHONE", or "BOTH".
     * Set at checkout time; defaults to "BOTH" if the buyer doesn't choose.
     */
    @Column(name = "otp_channel", length = 10)
    private String otpChannel;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Alias for `items` — used by ReviewService and any code expecting
     * the more descriptive "orderItems" accessor name.
     */
    public List<OrderItem> getOrderItems() {
        return this.items;
    }
}