package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.CheckoutRequest;
import com.example.freshfarm3.dto.response.CheckoutResponse;
import com.example.freshfarm3.dto.response.ShopOrderResponse;
import com.example.freshfarm3.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ── BUYER: Place Order ────────────────────────────────────────
    // POST /api/orders/checkout
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CheckoutResponse> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CheckoutRequest req) {
        CheckoutResponse response =
                orderService.placeOrder(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── BUYER: View My Orders ─────────────────────────────────────
    // GET /api/orders/my-orders
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<CheckoutResponse>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.getMyOrders(userDetails.getUsername()));
    }

    // ── BUYER: View Single Order / Track Order ────────────────────
    // GET /api/orders/{orderId}
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CheckoutResponse> getOrderById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(
                orderService.getOrderById(userDetails.getUsername(), orderId)
        );
    }

    // ── BUYER: Cancel Order ───────────────────────────────────────
    // PUT /api/orders/{orderId}/cancel
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CheckoutResponse> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(
                orderService.cancelOrder(userDetails.getUsername(), orderId)
        );
    }

    // ── SHOP: View Orders for My Products ───────────────────────
    // GET /api/orders/shop/my-orders
    @GetMapping("/shop/my-orders")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<List<ShopOrderResponse>> getOrdersForShop(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                orderService.getOrdersForShop(userDetails.getUsername())
        );
    }

    // ── ADMIN: View All Orders ────────────────────────────────────
    // GET /api/orders/admin/all
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CheckoutResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}