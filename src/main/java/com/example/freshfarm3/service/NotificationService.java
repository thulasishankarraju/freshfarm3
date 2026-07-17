package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.response.NotificationResponse;
import com.example.freshfarm3.entity.Notification;
import com.example.freshfarm3.entity.Order;
import com.example.freshfarm3.entity.OrderItem;
import com.example.freshfarm3.entity.User;
import com.example.freshfarm3.repository.NotificationRepository;
import com.example.freshfarm3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;
    private final EmailService           emailService;
    private final SmsService             smsService;

    // ── ORDER PLACED ──────────────────────────────────────────────
    /**
     * Called from OrderService after a successful order.
     * Notifies buyer + every unique shop involved.
     */
    @Transactional
    public void notifyOrderPlaced(Order order, List<OrderItem> items) {

        String orderNum = order.getOrderNumber();
        String amount   = "₹" + order.getTotalAmount();
        User   buyer    = order.getBuyer().getUser();

        // ── BUYER: DB Notification ──
        saveNotification(
                buyer,
                "Order Placed Successfully 🌿",
                "Your order #" + orderNum + " for " + amount + " has been placed!"
        );

        // ── BUYER: Email ──
        emailService.send(
                buyer.getEmail(),
                "FarmFresh — Order Confirmed 🌿",
                "Hi " + buyer.getFullName() + ",\n\n" +
                        "Your order #" + orderNum + " for " + amount + " has been placed successfully!\n" +
                        "We'll notify you as soon as the shop confirms it.\n\n" +
                        "Delivery to: " + order.getDeliveryAddress().getCity() + "\n\n" +
                        "Thank you for choosing FarmFresh 🌾\nTeam FarmFresh"
        );

        // ── BUYER: SMS ──
        if (buyer.getPhone() != null) {
            smsService.send(
                    "+91" + buyer.getPhone(),
                    "FarmFresh: Order #" + orderNum + " placed for " + amount +
                            ". Track at farmfresh.com"
            );
        }

        // ── ADMIN: Notify every admin that a new order needs confirming ──
        // (The shop is deliberately NOT notified at this stage — they only
        // hear about the order once an admin has reviewed and confirmed it,
        // see notifyShopsOrderConfirmed below.)
        List<User> admins = userRepository.findByRole(com.example.freshfarm3.enums.Role.ADMIN);
        admins.forEach(admin -> {
            saveNotification(
                    admin,
                    "New Order Placed 🛒",
                    "Order #" + orderNum + " from " + buyer.getFullName() + " for " + amount +
                            " is awaiting your confirmation."
            );

            emailService.send(
                    admin.getEmail(),
                    "FarmFresh — New Order Awaiting Confirmation",
                    "Hi " + admin.getFullName() + ",\n\n" +
                            "A new order #" + orderNum + " from " + buyer.getFullName() + " for " + amount +
                            " has been placed and is awaiting your confirmation.\n\n" +
                            "Team FarmFresh"
            );
        });

        log.info("Order-placed notifications sent for order: {}", orderNum);
    }

    // ── ORDER CONFIRMED (by admin) → notify shop(s) to start packing ──
    /**
     * Called from AdminService.confirmOrder once an admin approves the
     * order. Notifies every unique shop involved so they know to pack it.
     */
    @Transactional
    public void notifyShopsOrderConfirmed(Order order, List<OrderItem> items) {
        String orderNum = order.getOrderNumber();
        User   buyer     = order.getBuyer().getUser();

        items.stream()
                .map(oi -> oi.getProduct().getShop())
                .distinct()
                .forEach(shop -> {
                    User shopUser = shop.getUser();

                    saveNotification(
                            shopUser,
                            "Order Confirmed — Please Pack It 📦",
                            "Order #" + orderNum + " from " + buyer.getFullName() +
                                    " has been confirmed by the admin. Please pack it for delivery."
                    );

                    emailService.send(
                            shopUser.getEmail(),
                            "FarmFresh — Order Confirmed, Please Pack",
                            "Hi " + shopUser.getFullName() + ",\n\n" +
                                    "Order #" + orderNum + " from " + buyer.getFullName() +
                                    " has been confirmed by the admin.\n" +
                                    "Please pack it and mark it as packed in your dashboard.\n\n" +
                                    "Team FarmFresh"
                    );
                });
    }

    // ── ORDER PACKED (by shop) → notify buyer ──────────────────────
    @Transactional
    public void notifyOrderPacked(Order order) {
        User buyer = order.getBuyer().getUser();

        saveNotification(
                buyer,
                "Order Packed 📦",
                "Your order #" + order.getOrderNumber() + " has been packed and will be out for delivery soon."
        );

        emailService.send(
                buyer.getEmail(),
                "FarmFresh — Order Packed 📦",
                "Hi " + buyer.getFullName() + ",\n\n" +
                        "Your order #" + order.getOrderNumber() + " has been packed and is ready for dispatch.\n\n" +
                        "Team FarmFresh"
        );

        if (buyer.getPhone() != null) {
            smsService.send(
                    "+91" + buyer.getPhone(),
                    "FarmFresh: Order #" + order.getOrderNumber() + " has been packed."
            );
        }
    }

    // ── ORDER CONFIRMED ───────────────────────────────────────────
    @Transactional
    public void notifyOrderConfirmed(Order order) {
        User buyer = order.getBuyer().getUser();

        saveNotification(
                buyer,
                "Order Confirmed ✅",
                "Great news! Your order #" + order.getOrderNumber() + " has been confirmed by the shop."
        );

        emailService.send(
                buyer.getEmail(),
                "FarmFresh — Order Confirmed ✅",
                "Hi " + buyer.getFullName() + ",\n\n" +
                        "Your order #" + order.getOrderNumber() + " has been confirmed and is being prepared.\n\n" +
                        "Team FarmFresh"
        );

        if (buyer.getPhone() != null) {
            smsService.send(
                    "+91" + buyer.getPhone(),
                    "FarmFresh: Order #" + order.getOrderNumber() + " confirmed by shop!"
            );
        }
    }

    // ── ORDER CANCELLED ───────────────────────────────────────────
    @Transactional
    public void notifyOrderCancelled(Order order) {
        User buyer = order.getBuyer().getUser();

        saveNotification(
                buyer,
                "Order Cancelled ❌",
                "Your order #" + order.getOrderNumber() + " has been cancelled."
        );

        emailService.send(
                buyer.getEmail(),
                "FarmFresh — Order Cancelled",
                "Hi " + buyer.getFullName() + ",\n\n" +
                        "Your order #" + order.getOrderNumber() + " has been cancelled.\n" +
                        "If you paid online, a refund will be processed in 5–7 business days.\n\n" +
                        "Team FarmFresh"
        );

        if (buyer.getPhone() != null) {
            smsService.send(
                    "+91" + buyer.getPhone(),
                    "FarmFresh: Order #" + order.getOrderNumber() + " cancelled."
            );
        }
    }

    // ── SHOP PAYOUT: admin fixed an amount owed to the shop ─────────
    @Transactional
    public void notifyPayoutCreated(com.example.freshfarm3.entity.Shop shop, com.example.freshfarm3.entity.ShopPayout payout) {
        User shopUser = shop.getUser();
        String amount = "₹" + payout.getAmount();

        saveNotification(
                shopUser,
                "Payout Fixed 💰",
                "The admin has fixed a payout of " + amount + " for your shop. Status: Pending."
        );

        emailService.send(
                shopUser.getEmail(),
                "FarmFresh — Payout Fixed",
                "Hi " + shopUser.getFullName() + ",\n\n" +
                        "The admin has fixed a payout of " + amount + " for your shop's sales.\n" +
                        "It will be sent to your registered bank account shortly. You can track its status " +
                        "on your dashboard.\n\n" +
                        "Team FarmFresh"
        );
    }

    // ── SHOP PAYOUT: admin sent the money ────────────────────────────
    @Transactional
    public void notifyPayoutPaid(com.example.freshfarm3.entity.Shop shop, com.example.freshfarm3.entity.ShopPayout payout) {
        User shopUser = shop.getUser();
        String amount = "₹" + payout.getAmount();

        saveNotification(
                shopUser,
                "Payout Sent ✅",
                "Your payout of " + amount + " has been sent to your registered bank account."
        );

        emailService.send(
                shopUser.getEmail(),
                "FarmFresh — Payout Sent ✅",
                "Hi " + shopUser.getFullName() + ",\n\n" +
                        "Your payout of " + amount + " has been sent to your registered bank account.\n\n" +
                        "Team FarmFresh"
        );
    }

    // ── DELIVERY AGENT: earned a bonus ───────────────────────────────
    @Transactional
    public void notifyAgentBonus(User agentUser, java.math.BigDecimal bonusAmount, int deliveryCountToday) {
        saveNotification(
                agentUser,
                "Bonus Earned! 🎉",
                "You completed " + deliveryCountToday + " deliveries today and earned a ₹" + bonusAmount + " bonus!"
        );

        if (agentUser.getPhone() != null) {
            smsService.send(
                    "+91" + agentUser.getPhone(),
                    "FarmFresh: Great job! You earned a ₹" + bonusAmount + " bonus for " +
                            deliveryCountToday + " deliveries today."
            );
        }
    }

    // ── GET NOTIFICATIONS FOR USER ────────────────────────────────
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(String email) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(email);
    }

    // ── GET NOTIFICATIONS FOR USER, AS SAFE DTOs ────────────────────
    // Used by NotificationController — never return the Notification entity
    // directly, since its lazy `user` field would otherwise serialize the
    // buyer/agent/admin's User record (including the password hash) to JSON.
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String email) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── UNREAD COUNT FOR USER ───────────────────────────────────────
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    // ── MARK AS READ (single, owner-checked) ────────────────────────
    @Transactional
    public void markAsRead(String email, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        if (!n.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("This notification does not belong to you");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    // ── MARK ALL AS READ ─────────────────────────────────────────────
    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        notificationRepository.markAllAsReadForUser(user);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : null)
                .isRead(n.getIsRead())
                .referenceId(n.getReferenceId())
                .createdAt(n.getCreatedAt())
                .build();
    }

    // ── HELPER ───────────────────────────────────────────────────
    private void saveNotification(User user, String title, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setIsRead(false);
        notificationRepository.save(n);
    }
}
