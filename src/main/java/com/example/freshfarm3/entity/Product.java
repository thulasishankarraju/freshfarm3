package com.example.freshfarm3.entity;

import com.example.freshfarm3.enums.ProductGrade;
import com.example.freshfarm3.enums.UnitType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // Price per unit sold: per kg (KG), per piece (PIECE), or per liter (LITER).
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // How this product is sold & stocked. See UnitType for the rules.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UnitType unitType;

    // Only set (and only meaningful) when unitType == PIECE. The farmer
    // enters the average weight of a single piece so the system can
    // convert a piece-count order into a kg deduction from stock, since
    // stock for PIECE items is still tracked in kg, not piece-count.
    @Column(precision = 8, scale = 2)
    private BigDecimal avgPieceWeightGrams;

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
    // Stock is always kept in kg (for KG and PIECE products) or liters
    // (for LITER products) — never in piece-count — per the farmer's
    // physical stock-taking (they weigh/measure what's on the shelf).
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal stockQuantity;

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

    // ── Reviews: aggregate rating fields ────────────────────────
    @Column
    @Builder.Default
    private Double averageRating = 0.0;

    @Column
    @Builder.Default
    private Integer reviewCount = 0;
    // ────────────────────────────────────────────────────────────

    // ── Unit-aware quantity helpers ──────────────────────────────
    /**
     * The smallest step a buyer may order in, for this product's unit type.
     * KG / LITER → 0.25 (i.e. 250g / 250ml increments).
     * PIECE      → 1 (whole pieces only).
     */
    public BigDecimal stepSize() {
        return unitType == UnitType.PIECE ? BigDecimal.ONE : new BigDecimal("0.25");
    }

    /**
     * True if the requested order quantity is a valid multiple of this
     * product's step size (and is positive). Used to reject e.g. 0.3 kg
     * (not a multiple of 0.25) or 2.5 pieces (must be whole).
     */
    public boolean isValidOrderQuantity(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) return false;
        BigDecimal step = stepSize();
        BigDecimal remainder = qty.remainder(step);
        return remainder.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Converts an order quantity (in the buyer-facing unit — kg, pieces,
     * or liters) into the kg/liter amount to deduct from stockQuantity.
     * For PIECE products this multiplies by avgPieceWeightGrams; for
     * KG/LITER products the order quantity IS the stock quantity.
     */
    public BigDecimal stockDeltaFor(BigDecimal orderQty) {
        if (unitType == UnitType.PIECE) {
            BigDecimal avgWeightKg = (avgPieceWeightGrams != null ? avgPieceWeightGrams : BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
            return orderQty.multiply(avgWeightKg);
        }
        return orderQty;
    }

    /**
     * Approximate number of pieces left in stock, for display only
     * (e.g. "~48 pieces available"). Only meaningful for PIECE products.
     */
    public Integer approxPiecesAvailable() {
        if (unitType != UnitType.PIECE || avgPieceWeightGrams == null
                || avgPieceWeightGrams.compareTo(BigDecimal.ZERO) <= 0
                || stockQuantity == null) {
            return null;
        }
        BigDecimal avgWeightKg = avgPieceWeightGrams.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        return stockQuantity.divide(avgWeightKg, 0, RoundingMode.FLOOR).intValue();
    }

    // ── Sprint 3: Stock management helpers ──────────────────────
    /**
     * Returns true if the requested order quantity (buyer-facing unit)
     * can be fulfilled from current stock.
     * Called by CartService and OrderService before processing.
     */
    public boolean hasStock(BigDecimal requestedQty) {
        if (!Boolean.TRUE.equals(this.isAvailable) || this.stockQuantity == null || requestedQty == null) {
            return false;
        }
        BigDecimal delta = stockDeltaFor(requestedQty);
        return this.stockQuantity.compareTo(delta) >= 0;
    }

    /**
     * Deducts stock after a successful order. `qty` is in the buyer-facing
     * unit (kg, pieces, or liters) — it is converted to a kg/liter delta
     * internally before being subtracted from stockQuantity.
     * Automatically marks product unavailable and OUT_OF_STOCK when stock hits zero.
     * Throws if deduction would go negative (safety guard).
     */
    public void deductStock(BigDecimal qty) {
        BigDecimal delta = stockDeltaFor(qty);
        if (this.stockQuantity.compareTo(delta) < 0) {
            throw new IllegalStateException(
                    "Insufficient stock for product: " + this.name +
                            ". Available: " + this.stockQuantity + ", Requested: " + delta
            );
        }
        this.stockQuantity = this.stockQuantity.subtract(delta);
        if (this.stockQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            this.stockQuantity = BigDecimal.ZERO;
            this.isAvailable = false;
            this.status = ProductStatus.OUT_OF_STOCK;
        }
    }

    /**
     * Restores stock when an order is cancelled. `qty` is in the
     * buyer-facing unit, same conversion rule as deductStock.
     */
    public void restoreStock(BigDecimal qty) {
        BigDecimal delta = stockDeltaFor(qty);
        this.stockQuantity = this.stockQuantity.add(delta);
        this.isAvailable = true;
        if (this.status == ProductStatus.OUT_OF_STOCK) {
            this.status = ProductStatus.ACTIVE;
        }
    }
    // ────────────────────────────────────────────────────────────
}