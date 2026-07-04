package com.example.freshfarm3.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    // ── Who receives this notification ────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── Notification content ──────────────────────────────────
    @NotBlank(message = "Title is required")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Message is required")
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    // ── Type (ORDER_PLACED, PAYMENT_SUCCESS, etc.) ────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    @Builder.Default
    private NotificationType type = NotificationType.GENERAL;

    // ── Read status ───────────────────────────────────────────
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    // ── Optional reference to related entity (orderId, etc.) ──
    @Column(name = "reference_id")
    private Long referenceId;

    // ── Enum ──────────────────────────────────────────────────
    public enum NotificationType {
        GENERAL,
        ORDER_PLACED,
        ORDER_CONFIRMED,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PRODUCT_APPROVED,
        PRODUCT_REJECTED
    }
}
