package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.AdminProductRequest;
import com.example.freshfarm3.dto.request.PayoutRequest;
import com.example.freshfarm3.dto.request.ProductPriceRequest;
import com.example.freshfarm3.dto.response.DashboardStatsResponse;
import com.example.freshfarm3.dto.response.OrderResponse;
import com.example.freshfarm3.dto.response.PayoutResponse;
import com.example.freshfarm3.dto.response.ProductResponse;
import com.example.freshfarm3.service.AdminService;
import com.example.freshfarm3.service.PayoutService;
import com.example.freshfarm3.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService  adminService;
    private final ProductService productService;
    private final PayoutService  payoutService;

    /**
     * GET /api/admin/dashboard
     * Returns full platform dashboard statistics.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboard() {
        log.info("GET /api/admin/dashboard");
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    /**
     * GET /api/admin/shops/pending
     * Returns list of shops awaiting approval.
     */
    @GetMapping("/shops/pending")
    public ResponseEntity<List<DashboardStatsResponse.ShopSummary>> getPendingShops() {
        log.info("GET /api/admin/shops/pending");
        return ResponseEntity.ok(adminService.getPendingShops());
    }

    /**
     * PUT /api/admin/shops/{id}/approve
     * Approves a pending shop.
     */
    @PutMapping("/shops/{id}/approve")
    public ResponseEntity<DashboardStatsResponse.ShopSummary> approveShop(@PathVariable Long id) {
        log.info("PUT /api/admin/shops/{}/approve", id);
        return ResponseEntity.ok(adminService.approveShop(id));
    }

    /**
     * PUT /api/admin/shops/{id}/reject
     * Rejects a pending shop.
     */
    @PutMapping("/shops/{id}/reject")
    public ResponseEntity<DashboardStatsResponse.ShopSummary> rejectShop(@PathVariable Long id) {
        log.info("PUT /api/admin/shops/{}/reject", id);
        return ResponseEntity.ok(adminService.rejectShop(id));
    }

    /**
     * GET /api/admin/orders
     * Returns all platform orders.
     */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("GET /api/admin/orders");
        return ResponseEntity.ok(adminService.getAllOrders());
    }

    /**
     * PUT /api/admin/orders/{id}/confirm
     * Confirms a PENDING order so it can proceed to delivery assignment.
     */
    @PutMapping("/orders/{id}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable Long id) {
        log.info("PUT /api/admin/orders/{}/confirm", id);
        return ResponseEntity.ok(adminService.confirmOrder(id));
    }

    /**
     * GET /api/admin/statistics
     * Returns a compact platform statistics summary map.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("GET /api/admin/statistics");
        return ResponseEntity.ok(adminService.getPlatformStatistics());
    }

    // ═══════════════════════════════════════════════
    //  PRODUCT & PRICE MANAGEMENT
    //  Shops can add products but never set a price — only the admin
    //  can add a product with an immediate price, or fix the price on
    //  a product a shop already submitted.
    // ═══════════════════════════════════════════════

    /** GET /api/admin/products — every product, priced or not. */
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("GET /api/admin/products");
        return ResponseEntity.ok(productService.getAllProductsForAdmin());
    }

    /** GET /api/admin/products/pending-price — products awaiting a price. */
    @GetMapping("/products/pending-price")
    public ResponseEntity<List<ProductResponse>> getPendingPriceProducts() {
        log.info("GET /api/admin/products/pending-price");
        return ResponseEntity.ok(productService.getPendingPriceProducts());
    }

    /** POST /api/admin/products — admin adds a product directly, with a price, for a given shop. */
    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestPart("product") AdminProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        log.info("POST /api/admin/products for shop {}", request.getShopId());
        ProductResponse response = productService.createProductByAdmin(request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** PUT /api/admin/products/{id}/price — fix (or change) a product's selling price. */
    @PutMapping("/products/{id}/price")
    public ResponseEntity<ProductResponse> fixPrice(
            @PathVariable Long id, @Valid @RequestBody ProductPriceRequest request) {
        log.info("PUT /api/admin/products/{}/price -> {}", id, request.getPrice());
        return ResponseEntity.ok(productService.fixPrice(id, request));
    }

    // ═══════════════════════════════════════════════
    //  SHOP PAYOUTS / SETTLEMENTS
    //  Admin fixes the amount a shop is owed for their sales, then
    //  marks it paid once the money has been sent to the shop's
    //  registered bank account.
    // ═══════════════════════════════════════════════

    /** GET /api/admin/payouts — every payout, across every shop. */
    @GetMapping("/payouts")
    public ResponseEntity<List<PayoutResponse>> getAllPayouts() {
        log.info("GET /api/admin/payouts");
        return ResponseEntity.ok(payoutService.getAllPayouts());
    }

    /** GET /api/admin/shops/{shopId}/payouts — payouts for one shop. */
    @GetMapping("/shops/{shopId}/payouts")
    public ResponseEntity<List<PayoutResponse>> getPayoutsForShop(@PathVariable Long shopId) {
        log.info("GET /api/admin/shops/{}/payouts", shopId);
        return ResponseEntity.ok(payoutService.getPayoutsForShop(shopId));
    }

    /** POST /api/admin/shops/{shopId}/payouts — admin fixes the amount owed to a shop. */
    @PostMapping("/shops/{shopId}/payouts")
    public ResponseEntity<PayoutResponse> createPayout(
            @PathVariable Long shopId, @Valid @RequestBody PayoutRequest request) {
        log.info("POST /api/admin/shops/{}/payouts -> {}", shopId, request.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(payoutService.createPayout(shopId, request));
    }

    /** PUT /api/admin/payouts/{id}/pay — admin marks a payout as sent to the shop's bank account. */
    @PutMapping("/payouts/{id}/pay")
    public ResponseEntity<PayoutResponse> markPayoutPaid(@PathVariable Long id) {
        log.info("PUT /api/admin/payouts/{}/pay", id);
        return ResponseEntity.ok(payoutService.markPaid(id));
    }
}