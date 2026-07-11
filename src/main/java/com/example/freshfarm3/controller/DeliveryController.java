package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.DeliveryRequest;
import com.example.freshfarm3.dto.response.DeliveryResponse;
import com.example.freshfarm3.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    // POST /api/delivery/assign  — ADMIN
    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryResponse> assign(
            @Valid @RequestBody DeliveryRequest req) {
        return ResponseEntity.ok(deliveryService.assignDelivery(req));
    }

    // PUT /api/delivery/{deliveryId}/pickup  — AGENT
    @PutMapping("/{deliveryId}/pickup")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<DeliveryResponse> pickup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long deliveryId) {
        return ResponseEntity.ok(deliveryService.markPickedUp(userDetails.getUsername(), deliveryId));
    }

    // PUT /api/delivery/{deliveryId}/out-for-delivery  — AGENT
    @PutMapping("/{deliveryId}/out-for-delivery")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<DeliveryResponse> outForDelivery(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long deliveryId) {
        return ResponseEntity.ok(deliveryService.markOutForDelivery(userDetails.getUsername(), deliveryId));
    }

    // PUT /api/delivery/{deliveryId}/complete?otp=123456  — AGENT
    @PutMapping("/{deliveryId}/complete")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<DeliveryResponse> complete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long deliveryId,
            @RequestParam String otp) {
        return ResponseEntity.ok(
                deliveryService.completeDelivery(userDetails.getUsername(), deliveryId, otp)
        );
    }

    // GET /api/delivery/my-deliveries  — AGENT
    @GetMapping("/my-deliveries")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<List<DeliveryResponse>> myDeliveries(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(deliveryService.getMyDeliveries(userDetails.getUsername()));
    }

    // GET /api/delivery/track/{orderId}  — authenticated
    @GetMapping("/track/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DeliveryResponse> track(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.trackOrder(orderId));
    }
}