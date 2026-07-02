package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.SubscriptionRequest;
import com.example.freshfarm3.dto.response.SubscriptionResponse;
import com.example.freshfarm3.security.JwtUtil;
import com.example.freshfarm3.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final JwtUtil jwtUtil;

    /**
     * POST /api/subscriptions
     * Create a new subscription.
     */
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<SubscriptionResponse> create(
            @Valid @RequestBody SubscriptionRequest request,
            @RequestHeader("Authorization") String authHeader) {

        Long buyerUserId = extractUserId(authHeader);
        log.info("POST /api/subscriptions — buyerUserId={}", buyerUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.createSubscription(request, buyerUserId));
    }

    /**
     * PUT /api/subscriptions/{id}/pause
     */
    @PutMapping("/{id}/pause")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<SubscriptionResponse> pause(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        Long buyerUserId = extractUserId(authHeader);
        log.info("PUT /api/subscriptions/{}/pause — buyerUserId={}", id, buyerUserId);
        return ResponseEntity.ok(subscriptionService.pauseSubscription(id, buyerUserId));
    }

    /**
     * PUT /api/subscriptions/{id}/resume
     */
    @PutMapping("/{id}/resume")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<SubscriptionResponse> resume(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        Long buyerUserId = extractUserId(authHeader);
        log.info("PUT /api/subscriptions/{}/resume — buyerUserId={}", id, buyerUserId);
        return ResponseEntity.ok(subscriptionService.resumeSubscription(id, buyerUserId));
    }

    /**
     * DELETE /api/subscriptions/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<String> cancel(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        Long buyerUserId = extractUserId(authHeader);
        log.info("DELETE /api/subscriptions/{} — buyerUserId={}", id, buyerUserId);
        subscriptionService.cancelSubscription(id, buyerUserId);
        return ResponseEntity.ok("Subscription cancelled successfully");
    }

    /**
     * GET /api/subscriptions/my-subscriptions
     */
    @GetMapping("/my-subscriptions")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(
            @RequestHeader("Authorization") String authHeader) {

        Long buyerUserId = extractUserId(authHeader);
        log.info("GET /api/subscriptions/my-subscriptions — buyerUserId={}", buyerUserId);
        return ResponseEntity.ok(subscriptionService.getMySubscriptions(buyerUserId));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Long extractUserId(String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        return jwtUtil.extractUserId(token);
    }
}
