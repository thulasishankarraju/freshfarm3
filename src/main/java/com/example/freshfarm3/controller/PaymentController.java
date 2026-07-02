package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.PaymentRequest;
import com.example.freshfarm3.dto.request.PaymentVerifyRequest;
import com.example.freshfarm3.dto.response.PaymentResponse;
import com.example.freshfarm3.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // POST /api/payments/create-order
    @PostMapping("/create-order")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<PaymentResponse> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentRequest req) {
        return ResponseEntity.ok(
                paymentService.createRazorpayOrder(userDetails.getUsername(), req)
        );
    }

    // POST /api/payments/verify
    @PostMapping("/verify")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentVerifyRequest req) {
        return ResponseEntity.ok(
                paymentService.verifyPayment(userDetails.getUsername(), req)
        );
    }

    // GET /api/payments/{orderId}
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrder(orderId));
    }

    // POST /api/payments/refund  — Admin only
    @PostMapping("/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> refund(@RequestParam Long orderId) {
        return ResponseEntity.ok(paymentService.processRefund(orderId));
    }
}
