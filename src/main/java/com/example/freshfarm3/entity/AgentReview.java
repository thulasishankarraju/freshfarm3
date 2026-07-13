package com.example.freshfarm3.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AgentReview — a buyer's 1–5 star rating of the delivery agent who
 * delivered an order. Separate from Review (which rates the product/
 * farmer) since it's a different subject and a different cardinality:
 * one agent per order (via Delivery), vs. potentially several
 * products/farmers per order.
 */
@Entity
@Table(
        name = "agent_reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "buyer_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The delivery agent being rated
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private DeliveryAgent agent;

    // The buyer who wrote the review
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    // The order this review is tied to (one agent review per order)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Rating: 1 to 5
    @Column(nullable = false)
    private Integer rating;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @CreationTimestamp
    @Column(name = "review_date", nullable = false, updatable = false)
    private LocalDateTime reviewDate;
}
