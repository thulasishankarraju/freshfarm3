package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.response.ProductResponse;
import com.example.freshfarm3.entity.Farmer;
import com.example.freshfarm3.entity.Product;
import com.example.freshfarm3.entity.User;
import com.example.freshfarm3.repository.FarmerRepository;
import com.example.freshfarm3.repository.ProductRepository;
import com.example.freshfarm3.repository.UserRepository;
import com.example.freshfarm3.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/farmer")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
@Slf4j
public class FarmerController {

    private final ProductService    productService;
    private final ProductRepository productRepository;
    private final FarmerRepository  farmerRepository;
    private final UserRepository    userRepository;

    // ── Dashboard summary ─────────────────────────────────────

    /**
     * GET /api/farmer/dashboard
     * Returns basic stats for the farmer's dashboard.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Farmer farmer = getAuthenticatedFarmer();

        long activeCount = productRepository
                .countByFarmerAndStatus(farmer, Product.ProductStatus.ACTIVE);
        long inactiveCount = productRepository
                .countByFarmerAndStatus(farmer, Product.ProductStatus.INACTIVE);
        long outOfStockCount = productRepository
                .countByFarmerAndStatus(farmer, Product.ProductStatus.OUT_OF_STOCK);

        return ResponseEntity.ok(Map.of(
                "farmerId",        farmer.getId(),
                "farmerName",      farmer.getUser().getName(),
                "farmName",        farmer.getFarmName(),
                "activeProducts",  activeCount,
                "inactiveProducts",inactiveCount,
                "outOfStock",      outOfStockCount,
                "totalProducts",   activeCount + inactiveCount + outOfStockCount
        ));
    }

    // ── My products ───────────────────────────────────────────

    /**
     * GET /api/farmer/products
     * Lists all products belonging to the authenticated farmer.
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getMyProducts() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return ResponseEntity.ok(productService.getMyProducts(email));
    }

    // ── Private helper ────────────────────────────────────────

    private Farmer getAuthenticatedFarmer() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return farmerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException(
                        "Farmer profile not found for: " + email));
    }
}