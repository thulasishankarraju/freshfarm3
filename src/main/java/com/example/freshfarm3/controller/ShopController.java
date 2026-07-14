package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.response.ProductResponse;
import com.example.freshfarm3.entity.Shop;
import com.example.freshfarm3.entity.Product;
import com.example.freshfarm3.entity.User;
import com.example.freshfarm3.repository.ShopRepository;
import com.example.freshfarm3.repository.ProductRepository;
import com.example.freshfarm3.repository.UserRepository;
import com.example.freshfarm3.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
@PreAuthorize("hasRole('SHOP')")
@RequiredArgsConstructor
@Slf4j
public class ShopController {

    private final ProductService    productService;
    private final ProductRepository productRepository;
    private final ShopRepository  shopRepository;
    private final UserRepository    userRepository;

    // ── Dashboard summary ─────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Shop shop = getAuthenticatedShop();

        long activeCount = productRepository
                .countByShopAndStatus(shop, Product.ProductStatus.ACTIVE);
        long inactiveCount = productRepository
                .countByShopAndStatus(shop, Product.ProductStatus.INACTIVE);
        long outOfStockCount = productRepository
                .countByShopAndStatus(shop, Product.ProductStatus.OUT_OF_STOCK);
        long totalCount = productRepository.countByShop(shop);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("shopId", shop.getId());
        response.put("ownerName", shop.getUser().getFullName());
        response.put("shopName", shop.getShopName());
        response.put("activeProducts", activeCount);
        response.put("inactiveProducts", inactiveCount);
        response.put("outOfStock", outOfStockCount);
        response.put("totalProducts", totalCount);

        return ResponseEntity.ok(response);
    }

    // ── My products ───────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getMyProducts() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return ResponseEntity.ok(productService.getMyProducts(email));
    }

    // ── Private helper ────────────────────────────────────────

    private Shop getAuthenticatedShop() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return shopRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException(
                        "Shop profile not found for: " + email));
    }
}