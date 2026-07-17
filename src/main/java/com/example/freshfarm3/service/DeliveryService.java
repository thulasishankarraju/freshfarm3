package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.DeliveryRequest;
import com.example.freshfarm3.dto.response.AgentEarningsResponse;
import com.example.freshfarm3.dto.response.DeliveryResponse;
import com.example.freshfarm3.entity.AgentEarning;
import com.example.freshfarm3.entity.Delivery;
import com.example.freshfarm3.entity.DeliveryAgent;
import com.example.freshfarm3.entity.Notification;
import com.example.freshfarm3.entity.Order;
import com.example.freshfarm3.enums.AgentEarningType;
import com.example.freshfarm3.enums.DeliveryStatus;
import com.example.freshfarm3.enums.OrderStatus;
import com.example.freshfarm3.repository.AgentEarningRepository;
import com.example.freshfarm3.repository.DeliveryAgentRepository;
import com.example.freshfarm3.repository.DeliveryRepository;
import com.example.freshfarm3.repository.NotificationRepository;
import com.example.freshfarm3.repository.OrderRepository;
import com.example.freshfarm3.repository.UserRepository;
import com.example.freshfarm3.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
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
    private final UserRepository          userRepository;
    private final EmailService            emailService;
    private final SmsService              smsService;
    private final AgentEarningRepository  agentEarningRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    // Flat fee an agent earns for each delivery completed, on top of any
    // buyer tip on that order.
    private static final BigDecimal AGENT_BASE_DELIVERY_FEE = BigDecimal.valueOf(30);

    // Motivation bonus: every 5th delivery completed in a single calendar
    // day earns the agent an extra ₹9.
    private static final int        AGENT_BONUS_EVERY_N_DELIVERIES = 5;
    private static final BigDecimal AGENT_BONUS_AMOUNT             = BigDecimal.valueOf(9);

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
    public DeliveryResponse markPickedUp(String agentEmail, Long deliveryId) {
        Delivery delivery = getDeliveryForAgent(agentEmail, deliveryId);

        if (delivery.getDeliveryStatus() != DeliveryStatus.ASSIGNED) {
            throw new RuntimeException("Can only pick up an ASSIGNED delivery");
        }

        delivery.setDeliveryStatus(DeliveryStatus.PICKED_UP);
        delivery.setPickedUpAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        Order order = delivery.getOrder();
        // Order is now with the delivery agent, on its way to being
        // dispatched — buyer-visible progression: PROCESSING (packed) ->
        // SHIPPED (picked up) -> OUT_FOR_DELIVERY -> DELIVERED.
        order.setOrderStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);

        // Notify buyer — this was previously missing entirely.
        saveNotification(
                order.getBuyer().getUser(),
                "Order Picked Up 📦",
                "Your order #" + order.getOrderNumber() + " has been picked up by the delivery agent and will be on its way shortly."
        );
        emailService.send(
                order.getBuyer().getUser().getEmail(),
                "FarmFresh — Order Picked Up",
                "Hi " + order.getBuyer().getUser().getFullName() + ",\n\n" +
                        "Your order #" + order.getOrderNumber() + " has been picked up by the delivery agent.\n\n" +
                        "Team FarmFresh"
        );

        log.info("Order picked up: {}", order.getOrderNumber());
        return mapToResponse(delivery, true);   // show OTP to agent after pickup
    }

    // ── AGENT: OUT FOR DELIVERY ───────────────────────────────────
    @Transactional
    public DeliveryResponse markOutForDelivery(String agentEmail, Long deliveryId) {
        Delivery delivery = getDeliveryForAgent(agentEmail, deliveryId);

        if (delivery.getDeliveryStatus() != DeliveryStatus.PICKED_UP) {
            throw new RuntimeException("Must be in PICKED_UP state before going OUT_FOR_DELIVERY");
        }

        delivery.setDeliveryStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        deliveryRepository.save(delivery);

        Order order = delivery.getOrder();
        // Previously this incorrectly set OrderStatus.SHIPPED, which buyers
        // never see as a distinct "out for delivery" status. Use the enum
        // value that actually matches what's happening and what the buyer
        // sees on their order-tracking page.
        order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(order);

        // Notify buyer — resend the OTP now, at the moment it's actually
        // needed, in addition to when it was first issued at assignment.
        String otp = delivery.getOtp();
        saveNotification(
                order.getBuyer().getUser(),
                "Your Order Is On The Way! 🚚",
                "Order #" + order.getOrderNumber() + " is out for delivery. Your OTP is " + otp +
                        " — share it with the delivery agent to confirm receipt."
        );
        emailService.send(
                order.getBuyer().getUser().getEmail(),
                "FarmFresh — Order Out For Delivery",
                "Hi " + order.getBuyer().getUser().getFullName() + ",\n\n" +
                        "Your order #" + order.getOrderNumber() + " is out for delivery.\n" +
                        "Your OTP is: " + otp + "\n" +
                        "Please share this OTP ONLY with the delivery agent to confirm you received it.\n\n" +
                        "Team FarmFresh"
        );
        String buyerPhone = order.getBuyer().getUser().getPhone();
        if (buyerPhone != null) {
            smsService.send(
                    "+91" + buyerPhone,
                    "FarmFresh: Order #" + order.getOrderNumber() + " is out for delivery. Your OTP is " +
                            otp + ". Share only with your delivery agent."
            );
        }

        log.info("Order out for delivery: {}", order.getOrderNumber());
        return mapToResponse(delivery, true);
    }

    // ── AGENT: COMPLETE DELIVERY (OTP Verification) ───────────────
    @Transactional
    public DeliveryResponse completeDelivery(String agentEmail, Long deliveryId, String enteredOtp) {
        Delivery delivery = getDeliveryForAgent(agentEmail, deliveryId);

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

        // ── Record the agent's earning for this delivery, and award a
        // ── ₹9 bonus on every 5th delivery completed today.
        recordAgentEarningAndBonus(agent, delivery, order);

        // Notify buyer and shop
        notifyDeliveryComplete(order, agent);

        log.info("Order delivered: {}", order.getOrderNumber());
        return mapToResponse(delivery, false);
    }

    // ── AGENT EARNINGS: record delivery fee + daily bonus ───────────
    private void recordAgentEarningAndBonus(DeliveryAgent agent, Delivery delivery, Order order) {
        BigDecimal tip = order.getTipAmount() != null ? order.getTipAmount() : BigDecimal.ZERO;
        BigDecimal earningAmount = AGENT_BASE_DELIVERY_FEE.add(tip);

        agentEarningRepository.save(AgentEarning.builder()
                .agent(agent)
                .delivery(delivery)
                .type(AgentEarningType.DELIVERY_FEE)
                .amount(earningAmount)
                .note("Delivery fee for order #" + order.getOrderNumber())
                .build());

        // Count how many deliveries this agent has completed today
        // (including the one we just recorded) to see if a bonus is due.
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long deliveriesToday = deliveryRepository
                .findByDeliveryAgentAndDeliveryStatusAndDeliveredAtBetween(
                        agent, DeliveryStatus.DELIVERED, startOfDay, endOfDay)
                .size();

        if (deliveriesToday > 0 && deliveriesToday % AGENT_BONUS_EVERY_N_DELIVERIES == 0) {
            agentEarningRepository.save(AgentEarning.builder()
                    .agent(agent)
                    .delivery(delivery)
                    .type(AgentEarningType.BONUS)
                    .amount(AGENT_BONUS_AMOUNT)
                    .note("Bonus for completing " + deliveriesToday + " deliveries today")
                    .build());

            notifyAgentBonus(agent, (int) deliveriesToday);
            log.info("Agent {} earned ₹{} bonus for {} deliveries today",
                    agent.getUser().getEmail(), AGENT_BONUS_AMOUNT, deliveriesToday);
        }
    }

    private void notifyAgentBonus(DeliveryAgent agent, int deliveriesToday) {
        saveNotification(
                agent.getUser(),
                "Bonus Earned! 🎉",
                "You completed " + deliveriesToday + " deliveries today and earned a ₹" +
                        AGENT_BONUS_AMOUNT + " bonus!"
        );
        String phone = agent.getPhone();
        if (phone != null) {
            smsService.send(
                    "+91" + phone,
                    "FarmFresh: Great job! You earned a ₹" + AGENT_BONUS_AMOUNT + " bonus for " +
                            deliveriesToday + " deliveries today."
            );
        }
    }

    // ── AGENT: EARNINGS DASHBOARD ────────────────────────────────────
    @Transactional(readOnly = true)
    public AgentEarningsResponse getAgentEarningsSummary(String agentEmail) {
        DeliveryAgent agent = getAgent(agentEmail);

        List<AgentEarning> all = agentEarningRepository.findByAgentOrderByCreatedAtDesc(agent);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<AgentEarning> todayEarnings = agentEarningRepository
                .findByAgentAndCreatedAtBetween(agent, startOfDay, endOfDay);

        long todayDeliveries = deliveryRepository
                .findByDeliveryAgentAndDeliveryStatusAndDeliveredAtBetween(
                        agent, DeliveryStatus.DELIVERED, startOfDay, endOfDay)
                .size();

        BigDecimal todayTotal = todayEarnings.stream()
                .map(AgentEarning::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = all.stream()
                .map(AgentEarning::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBonus = all.stream()
                .filter(e -> e.getType() == AgentEarningType.BONUS)
                .map(AgentEarning::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalDeliveries = all.stream()
                .filter(e -> e.getType() == AgentEarningType.DELIVERY_FEE)
                .count();

        int untilNextBonus = (int) (AGENT_BONUS_EVERY_N_DELIVERIES -
                (todayDeliveries % AGENT_BONUS_EVERY_N_DELIVERIES));
        if (untilNextBonus == AGENT_BONUS_EVERY_N_DELIVERIES) {
            untilNextBonus = 0;
        }

        List<AgentEarningsResponse.EarningItem> recent = all.stream()
                .limit(20)
                .map(e -> AgentEarningsResponse.EarningItem.builder()
                        .id(e.getId())
                        .type(e.getType().name())
                        .amount(e.getAmount())
                        .orderNumber(e.getDelivery() != null ? e.getDelivery().getOrder().getOrderNumber() : null)
                        .note(e.getNote())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return AgentEarningsResponse.builder()
                .todayDeliveries(todayDeliveries)
                .todayEarnings(todayTotal)
                .totalDeliveries(totalDeliveries)
                .totalEarnings(grandTotal)
                .totalBonus(totalBonus)
                .deliveriesUntilNextBonus(untilNextBonus)
                .recent(recent)
                .build();
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

    private Delivery getDeliveryForAgent(String agentEmail, Long deliveryId) {
        DeliveryAgent agent = getAgent(agentEmail);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + deliveryId));

        if (!delivery.getDeliveryAgent().getId().equals(agent.getId())) {
            throw new RuntimeException("This delivery is not assigned to you");
        }
        return delivery;
    }

    private void notifyBuyer(Order order, DeliveryAgent agent, String otp) {
        String buyerEmail = order.getBuyer().getUser().getEmail();
        String buyerPhone = order.getBuyer().getUser().getPhone();
        String agentName  = agent.getUser().getFullName();
        String agentPhone = agent.getPhone();
        String channel     = order.getOtpChannel() != null ? order.getOtpChannel() : "BOTH";

        saveNotification(
                order.getBuyer().getUser(),
                "Delivery Agent Assigned 🚚",
                "Order #" + order.getOrderNumber() + " is assigned to " + agentName +
                        (agentPhone != null ? " (" + agentPhone + ")" : "") +
                        ". Your OTP: " + otp
        );

        boolean sendEmail = channel.equals("EMAIL") || channel.equals("BOTH");
        boolean sendSms   = channel.equals("PHONE") || channel.equals("BOTH");

        if (sendEmail) {
            emailService.send(
                    buyerEmail,
                    "FarmFresh — Your Delivery Agent Is Assigned",
                    "Hi " + order.getBuyer().getUser().getFullName() + ",\n\n" +
                            "Your order #" + order.getOrderNumber() + " has been assigned to " +
                            agentName + " (" + agent.getVehicleNumber() + ").\n" +
                            (agentPhone != null ? "Agent contact number: " + agentPhone +
                                                  " — call or message them directly for accurate delivery.\n\n" : "\n") +
                            "Your delivery OTP is: " + otp + "\n" +
                            "Please share this OTP ONLY with the delivery agent at the time of delivery.\n\n" +
                            "Estimated delivery time: within 3 hours.\n\nTeam FarmFresh"
            );
        }

        if (sendSms && buyerPhone != null) {
            smsService.send("+91" + buyerPhone,
                    "FarmFresh: Order #" + order.getOrderNumber() + " assigned to " + agentName +
                            (agentPhone != null ? " (" + agentPhone + ")" : "") +
                            ". Your OTP is " + otp + ". Share only with your delivery agent.");
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
                        "Deliver to: " + order.getDeliveryAddress().getAddressLine() + ", " +
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

        // Notify shops in the order
        order.getItems().forEach(oi -> {
            saveNotification(
                    oi.getProduct().getShop().getUser(),
                    "Order Completed ✅",
                    "Order #" + order.getOrderNumber() + " has been delivered to the buyer."
            );
        });

        // Notify admins — so admin knows the delivery agent has confirmed delivery.
        userRepository.findByRole(Role.ADMIN).forEach(admin ->
                saveNotification(
                        admin,
                        "Delivery Completed ✅",
                        "Order #" + order.getOrderNumber() + " was delivered by " +
                                agent.getUser().getFullName() + " and confirmed via OTP."
                )
        );
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