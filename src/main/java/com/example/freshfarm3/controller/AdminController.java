package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.response.DashboardStatsResponse;
import com.example.freshfarm3.dto.response.OrderResponse;
import com.example.freshfarm3.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

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
}