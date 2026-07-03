package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.DeliveryRequest;
import com.example.freshfarm3.dto.response.DeliveryResponse;
import com.example.freshfarm3.entity.Delivery;
import com.example.freshfarm3.entity.DeliveryAgent;
import com.example.freshfarm3.entity.Notification;
import com.example.freshfarm3.entity.Order;
import com.example.freshfarm3.enums.DeliveryStatus;
import com.example.freshfarm3.enums.OrderStatus;
import com.example.freshfarm3.repository.DeliveryAgentRepository;
import com.example.freshfarm3.repository.DeliveryRepository;
import com.example.freshfarm3.repository.NotificationRepository;
import com.example.freshfarm3.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository      deliveryRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final OrderRepository         orderRepository;
    private final NotificationRepository  notificationRepository;
    private final EmailService            emailService;
    private final SmsService              smsService;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ── ASSIGN DELIVERY ───────────────────────────────────────────
    @Transactional
    public DeliveryResponse assignDelivery(DeliveryRequest req) {

        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + req.getOrderId()));

        if (order.getOrderStatus() != OrderStatus.CONFIRMED &&
                order.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new RuntimeException(
                    "Delivery can only be assigned to CONFIRMED or PROCESSING orders. " +
                            "Current status: " + order.getOrderStatus()
            );
        }

        if (deliveryRepository.findByOrder_Id(order.getId()).isPresent()) {
            throw new RuntimeException("Delivery already assigned for order: " + order.getOrderNumber());
        }

        // Find agent — explicit or auto-assign
        DeliveryAgent agent;
        if (req.getAgentId() != null) {
            agent = deliveryAgentRepository.findById(req.getAgentId())
                    .orElseThrow(() -> new RuntimeException("Agent not found: " + req.getAgentId()));
            if (!agent.getIsAvailable()) {
                throw new RuntimeException("Agent is not available right now");
            }
        } else {
            List<DeliveryAgent> available = deliveryAgentRepository.findByIsAvailableTrue();
            if (available.isEmpty()) {
                throw new RuntimeException("No delivery agents are currently available");
            }
            agent = available.get(0);   // Simple round-robin; Sprint 5+ can add proximity logic
        }

        // Generate OTP
        String otp = generateOtp();

        // Create Delivery record
        Delivery delivery = Delivery.builder()
                .order(order)
                .deliveryAgent(agent)
                .otp(otp)
                .assignedAt(LocalDateTime.now())
                .deliveryStatus(DeliveryStatus.ASSIGNED)
                .estimatedDeliveryTime(LocalDateTime.now().plusHours(3))
                .otpVerified(false)
                .build();
        deliveryRepository.save(delivery);

        // Mark agent unavailable
        agent.setIsAvailable(false);
        deliveryAgentRepository.save(agent);

        // Update order status
        order.setOrderStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        // Notify buyer
        notifyBuyer(order, agent, otp);

        // Notify agent
        notifyAgent(order, agent, otp);

        log.info("Delivery assigned: order={}, agent={}", order.getOrderNumber(), agent.getUser().getEmail());
        return mapToResponse(delivery, false);  // hide OTP from this response
    }

    // ── AGENT: PICKUP ─────────────────────────────────────────────
    @Transactional
    public DeliveryResponse markPickedUp(String agentEmail) {
        Delivery delivery = getActiveDeliveryForAgent(agentEmail);

        if (delivery.getDeliveryStatus() != DeliveryStatus.ASSIGNED) {
            throw new RuntimeException("Can only pick up an ASSIGNED delivery");
        }

        delivery.setDeliveryStatus(DeliveryStatus.PICKED_UP);
        delivery.setPickedUpAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        Order order = delivery.getOrder();
        log.info("Order picked up: {}", order.getOrderNumber());
        return mapToResponse(delivery, true);   // show OTP to agent after pickup
    }

    // ── AGENT: OUT FOR DELIVERY ───────────────────────────────────
    @Transactional
    public DeliveryResponse markOutForDelivery(String agentEmail) {
        Delivery delivery = getActiveDeliveryForAgent(agentEmail);

        if (delivery.getDeliveryStatus() != DeliveryStatus.PICKED_UP) {
            throw new RuntimeException("Must be in PICKED_UP state before going OUT_FOR_DELIVERY");
        }

        delivery.setDeliveryStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        deliveryRepository.save(delivery);

        Order order = delivery.getOrder();
        order.setOrderStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);

        // Notify buyer
        saveNotification(
                order.getBuyer().getUser(),
                "Your Order Is On The Way! 🚚",
                "Order #" + order.getOrderNumber() + " is out for delivery. Prepare your OTP."
        );

        log.info("Order out for delivery: {}", order.getOrderNumber());
        return mapToResponse(delivery, true);
    }

    // ── AGENT: COMPLETE DELIVERY (OTP Verification) ───────────────
    @Transactional
    public DeliveryResponse completeDelivery(String agentEmail, String enteredOtp) {
        Delivery delivery = getActiveDeliveryForAgent(agentEmail);

        if (delivery.getDeliveryStatus() != DeliveryStatus.OUT_FOR_DELIVERY) {
            throw new RuntimeException("Delivery must be OUT_FOR_DELIVERY to complete it");
        }

        // Verify OTP
        if (!delivery.getOtp().equals(enteredOtp)) {
            throw new RuntimeException("Invalid OTP. Please ask the buyer for the correct OTP.");
        }

        // Mark delivered
        delivery.setDeliveryStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());
        delivery.setOtpVerified(true);
        deliveryRepository.save(delivery);

        // Update order status
        Order order = delivery.getOrder();
        order.setOrderStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        // Mark agent available again
        DeliveryAgent agent = delivery.getDeliveryAgent();
        agent.setIsAvailable(true);
        deliveryAgentRepository.save(agent);

        // Notify buyer and farmer
        notifyDeliveryComplete(order, agent);

        log.info("Order delivered: {}", order.getOrderNumber());
        return mapToResponse(delivery, false);
    }

    // ── GET AGENT'S DELIVERIES ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DeliveryResponse> getMyDeliveries(String agentEmail) {
        DeliveryAgent agent = getAgent(agentEmail);
        return deliveryRepository.findByDeliveryAgent(agent).stream()
                .map(d -> mapToResponse(d, true))
                .collect(Collectors.toList());
    }

    // ── TRACK ORDER ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public DeliveryResponse trackOrder(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new RuntimeException("No delivery found for order: " + orderId));
        return mapToResponse(delivery, false);   // OTP hidden from buyer
    }

    // ── HELPERS ───────────────────────────────────────────────────
    private String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private DeliveryAgent getAgent(String email) {
        return deliveryAgentRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Delivery agent not found: " + email));
    }

    private Delivery getActiveDeliveryForAgent(String agentEmail) {
        DeliveryAgent agent = getAgent(agentEmail);
        return deliveryRepository
                .findByDeliveryAgentAndDeliveryStatusNot(agent, DeliveryStatus.DELIVERED)
                .stream()
                .filter(d -> d.getDeliveryStatus() != DeliveryStatus.CANCELLED)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active delivery found for this agent"));
    }

    private void notifyBuyer(Order order, DeliveryAgent agent, String otp) {
        String buyerEmail = order.getBuyer().getUser().getEmail();
        String buyerPhone = order.getBuyer().getUser().getPhone();
        String agentName  = agent.getUser().getFullName();

        saveNotification(
                order.getBuyer().getUser(),
                "Delivery Agent Assigned 🚚",
                "Order #" + order.getOrderNumber() + " is assigned to " + agentName +
                        ". Your OTP: " + otp
        );

        emailService.send(
                buyerEmail,
                "FarmFresh — Your Delivery Agent Is Assigned",
                "Hi " + order.getBuyer().getUser().getFullName() + ",\n\n" +
                        "Your order #" + order.getOrderNumber() + " has been assigned to " +
                        agentName + " (" + agent.getVehicleNumber() + ").\n\n" +
                        "Your delivery OTP is: " + otp + "\n" +
                        "Please share this OTP ONLY with the delivery agent at the time of delivery.\n\n" +
                        "Estimated delivery time: within 3 hours.\n\nTeam FarmFresh"
        );

        if (buyerPhone != null) {
            smsService.send("+91" + buyerPhone,
                    "FarmFresh: Your OTP for order #" + order.getOrderNumber() +
                            " is " + otp + ". Share only with your delivery agent.");
        }
    }

    private void notifyAgent(Order order, DeliveryAgent agent, String otp) {
        String agentEmail = agent.getUser().getEmail();
        String agentPhone = agent.getPhone();
        String address    = order.getDeliveryAddress().getCity();

        saveNotification(
                agent.getUser(),
                "New Delivery Assigned 📦",
                "Order #" + order.getOrderNumber() + " assigned to you. Deliver to " + address
        );

        emailService.send(
                agentEmail,
                "FarmFresh — New Delivery Assignment",
                "Hi " + agent.getUser().getFullName()+ ",\n\n" +
                        "You have a new delivery assignment.\n" +
                        "Order: #" + order.getOrderNumber() + "\n" +
                        "Deliver to: " + order.getDeliveryAddress().getStreet() + ", " +
                        order.getDeliveryAddress().getCity() + "\n" +
                        "Buyer OTP: " + otp + " (buyer will share this with you at delivery)\n\n" +
                        "Team FarmFresh"
        );

        if (agentPhone != null) {
            smsService.send("+91" + agentPhone,
                    "FarmFresh: New order #" + order.getOrderNumber() +
                            " assigned. Deliver to " + address);
        }
    }

    private void notifyDeliveryComplete(Order order, DeliveryAgent agent) {
        // Notify buyer
        saveNotification(
                order.getBuyer().getUser(),
                "Order Delivered! 🎉",
                "Your order #" + order.getOrderNumber() + " has been delivered successfully!"
        );
        emailService.send(
                order.getBuyer().getUser().getEmail(),
                "FarmFresh — Order Delivered! 🎉",
                "Hi " + order.getBuyer().getUser().getFullName() + ",\n\n" +
                        "Your order #" + order.getOrderNumber() + " has been delivered.\n" +
                        "Thank you for shopping with FarmFresh! 🌿\nTeam FarmFresh"
        );

        // Notify farmers in the order
        order.getItems().forEach(oi -> {
            saveNotification(
                    oi.getProduct().getFarmer().getUser(),
                    "Order Completed ✅",
                    "Order #" + order.getOrderNumber() + " has been delivered to the buyer."
            );
        });
    }

    private void saveNotification(com.example.freshfarm3.entity.User user, String title, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setIsRead(false);
        notificationRepository.save(n);
    }

    private DeliveryResponse mapToResponse(Delivery d, boolean includeOtp) {
        DeliveryAgent agent = d.getDeliveryAgent();
        return DeliveryResponse.builder()
                .deliveryId(d.getId())
                .orderId(d.getOrder().getId())
                .orderNumber(d.getOrder().getOrderNumber())
                .agentId(agent.getId())
                .agentName(agent.getUser().getFullName())
                .agentPhone(agent.getPhone())
                .vehicleNumber(agent.getVehicleNumber())
                .vehicleType(agent.getVehicleType())
                .deliveryStatus(d.getDeliveryStatus())
                .assignedAt(d.getAssignedAt())
                .pickedUpAt(d.getPickedUpAt())
                .deliveredAt(d.getDeliveredAt())
                .estimatedDeliveryTime(d.getEstimatedDeliveryTime())
                .otpVerified(d.getOtpVerified())
                .otp(includeOtp ? d.getOtp() : null)
                .build();
    }
}
