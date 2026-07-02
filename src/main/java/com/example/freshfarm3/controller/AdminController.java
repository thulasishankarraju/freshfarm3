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
     * GET /api/admin/farmers/pending
     * Returns list of farmers awaiting approval.
     */
    @GetMapping("/farmers/pending")
    public ResponseEntity<List<DashboardStatsResponse.FarmerSummary>> getPendingFarmers() {
        log.info("GET /api/admin/farmers/pending");
        return ResponseEntity.ok(adminService.getPendingFarmers());
    }

    /**
     * PUT /api/admin/farmers/{id}/approve
     * Approves a pending farmer.
     */
    @PutMapping("/farmers/{id}/approve")
    public ResponseEntity<DashboardStatsResponse.FarmerSummary> approveFarmer(@PathVariable Long id) {
        log.info("PUT /api/admin/farmers/{}/approve", id);
        return ResponseEntity.ok(adminService.approveFarmer(id));
    }

    /**
     * PUT /api/admin/farmers/{id}/reject
     * Rejects a pending farmer.
     */
    @PutMapping("/farmers/{id}/reject")
    public ResponseEntity<DashboardStatsResponse.FarmerSummary> rejectFarmer(@PathVariable Long id) {
        log.info("PUT /api/admin/farmers/{}/reject", id);
        return ResponseEntity.ok(adminService.rejectFarmer(id));
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
     * GET /api/admin/statistics
     * Returns a compact platform statistics summary map.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("GET /api/admin/statistics");
        return ResponseEntity.ok(adminService.getPlatformStatistics());
    }
}