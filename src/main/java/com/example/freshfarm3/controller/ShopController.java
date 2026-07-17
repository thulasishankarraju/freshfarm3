package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.response.PayoutResponse;
import com.example.freshfarm3.dto.response.ProductResponse;
import com.example.freshfarm3.entity.Shop;
import com.example.freshfarm3.entity.Product;
import com.example.freshfarm3.entity.User;
import com.example.freshfarm3.enums.PayoutStatus;
import com.example.freshfarm3.repository.ShopPayoutRepository;
import com.example.freshfarm3.repository.ShopRepository;
import com.example.freshfarm3.repository.ProductRepository;
import com.example.freshfarm3.repository.UserRepository;
import com.example.freshfarm3.service.PayoutService;
import com.example.freshfarm3.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
@PreAuthorize("hasRole('SHOP')")
@RequiredArgsConstructor
@Slf4j
public class ShopController {

    private final ProductService       productService;
    private final ProductRepository    productRepository;
    private final ShopRepository       shopRepository;
    private final UserRepository       userRepository;
    private final PayoutService        payoutService;
    private final ShopPayoutRepository shopPayoutRepository;

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

        // Payout / settlement summary — how much the admin has fixed as
        // owed to this shop, and how much of that has actually been paid.
        BigDecimal pendingPayout = shopPayoutRepository.totalForShopByStatus(shop, PayoutStatus.PENDING);
        BigDecimal paidPayout = shopPayoutRepository.totalForShopByStatus(shop, PayoutStatus.PAID);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("shopId", shop.getId());
        response.put("ownerName", shop.getUser().getFullName());
        response.put("shopName", shop.getShopName());
        response.put("activeProducts", activeCount);
        response.put("inactiveProducts", inactiveCount);
        response.put("outOfStock", outOfStockCount);
        response.put("totalProducts", totalCount);
        response.put("pendingPayoutAmount", pendingPayout);
        response.put("paidPayoutAmount", paidPayout);

        return ResponseEntity.ok(response);
    }

    // ── My products ───────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getMyProducts() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return ResponseEntity.ok(productService.getMyProducts(email));
    }

    // ── My payouts ────────────────────────────────────────────

    @GetMapping("/payouts")
    public ResponseEntity<List<PayoutResponse>> getMyPayouts() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return ResponseEntity.ok(payoutService.getMyPayouts(email));
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