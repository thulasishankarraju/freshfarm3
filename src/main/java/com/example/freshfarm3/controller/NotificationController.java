package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.response.NotificationResponse;
import com.example.freshfarm3.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * NotificationController — in-app notification feed for every role.
 *
 * NOTE: Notification, NotificationService, and NotificationRepository already
 * existed (buyer/farmer/admin notifications were being saved to the DB for
 * order-placed, order-confirmed, delivery-assigned, and delivery-completed
 * events), but nothing ever exposed them over the API — so the buyer never
 * saw "delivery agent assigned" in-app, and the admin never saw "order
 * delivered" in-app either, even though the emails/SMS were going out. This
 * controller fills that gap. Open to any authenticated user (BUYER, FARMER,
 * ADMIN, AGENT) since notifications are scoped to the caller's own account.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    // GET /api/notifications — current user's notifications, newest first
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> myNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userDetails.getUsername()));
    }

    // GET /api/notifications/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of("unread", notificationService.getUnreadCount(userDetails.getUsername())));
    }

    // PUT /api/notifications/{id}/read
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        notificationService.markAsRead(userDetails.getUsername(), id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    // PUT /api/notifications/read-all
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
