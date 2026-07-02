package com.example.freshfarm3.entity;

import com.freshfarm3.enums.ProductGrade;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String unit;           // e.g. "kg", "dozen", "piece"

    // ── Status ───────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    public enum ProductStatus {
        ACTIVE, INACTIVE, OUT_OF_STOCK
    }
    // ────────────────────────────────────────────────────────────

    // ── Sprint 3: Inventory fields ──────────────────────────────
    @Column(nullable = false)
    private Integer stockQuantity; // how many units are in stock

    @Column(nullable = false)
    private Boolean isAvailable;   // false when stock = 0 or farmer delists
    // ────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductGrade grade;    // A, B, C

    @Column(length = 100)
    private String origin;         // e.g. "Nashik, Maharashtra"

    @Column(nullable = false)
    private Boolean isOrganic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    // ── Sprint 3: Stock management helpers ──────────────────────
    /**
     * Returns true if the requested quantity can be fulfilled.
     * Called by CartService and OrderService before processing.
     */
    public boolean hasStock(int requestedQty) {
        return Boolean.TRUE.equals(this.isAvailable)
                && this.stockQuantity != null
                && this.stockQuantity >= requestedQty;
    }

    /**
     * Deducts stock after a successful order.
     * Automatically marks product unavailable and OUT_OF_STOCK when stock hits zero.
     * Throws if deduction would go negative (safety guard).
     */
    public void deductStock(int qty) {
        if (this.stockQuantity < qty) {
            throw new IllegalStateException(
                    "Insufficient stock for product: " + this.name +
                            ". Available: " + this.stockQuantity + ", Requested: " + qty
            );
        }
        this.stockQuantity -= qty;
        if (this.stockQuantity == 0) {
            this.isAvailable = false;
            this.status = ProductStatus.OUT_OF_STOCK;
        }
    }

    /**
     * Restores stock when an order is cancelled.
     */
    public void restoreStock(int qty) {
        this.stockQuantity += qty;
        this.isAvailable = true;
        if (this.status == ProductStatus.OUT_OF_STOCK) {
            this.status = ProductStatus.ACTIVE;
        }
    }
    // ────────────────────────────────────────────────────────────
}