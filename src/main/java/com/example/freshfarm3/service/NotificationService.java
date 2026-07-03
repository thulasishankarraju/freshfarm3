package com.example.freshfarm3.service;

import com.example.freshfarm3.entity.Order;
import com.example.freshfarm3.entity.OrderItem;
import com.example.freshfarm3.entity.User;
import com.example.freshfarm3.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService           emailService;
    private final SmsService             smsService;

    // ── ORDER PLACED ──────────────────────────────────────────────
    /**
     * Called from OrderService after a successful order.
     * Notifies buyer + every unique farmer involved.
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
                        "We'll notify you as soon as the farmer confirms it.\n\n" +
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

        // ── FARMERS: Notify each unique farmer ──
        items.stream()
                .map(oi -> oi.getProduct().getFarmer())
                .distinct()
                .forEach(farmer -> {
                    User farmerUser = farmer.getUser();

                    saveNotification(
                            farmerUser,
                            "New Order Received! 🧑‍🌾",
                            "New order #" + orderNum + " from " + buyer.getFullName() +
                                    ". Please confirm in your dashboard."
                    );

                    emailService.send(
                            farmerUser.getEmail(),
                            "FarmFresh — New Order for You! 🧑‍🌾",
                            "Hi " + farmerUser.getFullName() + ",\n\n" +
                                    "You have a new order #" + orderNum + " from " + buyer.getFullName() + ".\n" +
                                    "Please log in and confirm it so we can dispatch it.\n\n" +
                                    "Team FarmFresh"
                    );
                });

        log.info("Order-placed notifications sent for order: {}", orderNum);
    }

    // ── ORDER CONFIRMED ───────────────────────────────────────────
    @Transactional
    public void notifyOrderConfirmed(Order order) {
        User buyer = order.getBuyer().getUser();

        saveNotification(
                buyer,
                "Order Confirmed ✅",
                "Great news! Your order #" + order.getOrderNumber() + " has been confirmed by the farmer."
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
                    "FarmFresh: Order #" + order.getOrderNumber() + " confirmed by farmer!"
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

    // ── GET NOTIFICATIONS FOR USER ────────────────────────────────
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(String email) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(email);
    }

    // ── MARK AS READ ──────────────────────────────────────────────
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
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
