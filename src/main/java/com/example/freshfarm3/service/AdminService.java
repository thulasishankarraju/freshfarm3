package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.response.DashboardStatsResponse;
import com.example.freshfarm3.entity.User;
import com.example.freshfarm3.enums.OrderStatus;
import com.example.freshfarm3.repository.*;
import com.example.freshfarm3.dto.response.FarmerResponse;
import com.example.freshfarm3.dto.response.OrderResponse;
import com.example.freshfarm3.entity.Farmer;
import com.example.freshfarm3.entity.Notification;
import com.example.freshfarm3.exception.ResourceNotFoundException;
import com.example.freshfarm3.exception.ValidationException;
import com.example.freshfarm3.repository.*;
import com.example.freshfarm3.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final FarmerRepository farmerRepository;
    private final BuyerRepository buyerRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final ReviewRepository        reviewRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationRepository  notificationRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  DASHBOARD STATISTICS
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        log.info("Building dashboard statistics");

        LocalDate     today        = LocalDate.now();
        LocalDateTime startOfDay   = today.atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime now          = LocalDateTime.now();

        // User counts
        long totalBuyers   = buyerRepository.count();
        long totalFarmers  = farmerRepository.count();
        long totalUsers    = userRepository.count();
        long pendingFarmers  = farmerRepository.countByApprovalStatus("PENDING");
        long approvedFarmers = farmerRepository.countByApprovalStatus("APPROVED");

        // Product counts
        long totalProducts  = productRepository.count();
        long activeProducts = productRepository.countByAvailableTrue();
        double avgRating    = productRepository.findAverageProductRating();

        // Order counts
        long ordersToday      = orderRepository.countByOrderDateBetween(startOfDay, now);
        long ordersThisMonth  = orderRepository.countByOrderDateBetween(startOfMonth, now);
        long totalOrders      = orderRepository.count();
        long deliveredOrders  = orderRepository.countByOrderStatus(OrderStatus.DELIVERED);
        long pendingDeliveries = orderRepository.countByOrderStatus(OrderStatus.OUT_FOR_DELIVERY);

        // Revenue
        BigDecimal totalRevenue     = orderRepository.sumTotalAmountByPaymentStatus("PAID");
        BigDecimal revenueToday     = orderRepository.sumTotalAmountByPaymentStatusAndDateBetween("PAID", startOfDay, now);
        BigDecimal revenueThisMonth = orderRepository.sumTotalAmountByPaymentStatusAndDateBetween("PAID", startOfMonth, now);

        // Delivery agents
        long activeAgents = deliveryAgentRepository.countByActiveTrue();

        // Reviews
        long totalReviews = reviewRepository.count();

        // Subscriptions
        long activeSubscriptions = subscriptionRepository.countByActiveTrue();

        // Charts
        List<Map<String, Object>> revenueByMonth = buildRevenueByMonth();
        List<Map<String, Object>> ordersByMonth  = buildOrdersByMonth();
        List<Map<String, Object>> topProducts    = buildTopProducts();
        List<Map<String, Object>> topFarmers     = buildTopFarmers();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalBuyers(totalBuyers)
                .totalFarmers(totalFarmers)
                .pendingFarmers(pendingFarmers)
                .approvedFarmers(approvedFarmers)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .averageProductRating(avgRating)
                .ordersToday(ordersToday)
                .ordersThisMonth(ordersThisMonth)
                .totalOrders(totalOrders)
                .deliveredOrders(deliveredOrders)
                .pendingDeliveries(pendingDeliveries)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .revenueToday(revenueToday != null ? revenueToday : BigDecimal.ZERO)
                .revenueThisMonth(revenueThisMonth != null ? revenueThisMonth : BigDecimal.ZERO)
                .activeDeliveryAgents(activeAgents)
                .totalReviews(totalReviews)
                .activeSubscriptions(activeSubscriptions)
                .revenueByMonth(revenueByMonth)
                .ordersByMonth(ordersByMonth)
                .topProducts(topProducts)
                .topFarmers(topFarmers)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PENDING FARMERS
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DashboardStatsResponse.FarmerSummary> getPendingFarmers() {
        return farmerRepository.findByApprovalStatus("PENDING")
                .stream()
                .map(this::mapToFarmerSummary)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  APPROVE FARMER
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public DashboardStatsResponse.FarmerSummary approveFarmer(Long farmerId) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + farmerId));

        if ("APPROVED".equals(farmer.getApprovalStatus())) {
            throw new ValidationException("Farmer is already approved");
        }

        farmer.setApprovalStatus("APPROVED");
        farmer = farmerRepository.save(farmer);

        sendNotificationToUser(farmer.getUser(),
                "Account Approved 🎉",
                "Congratulations! Your FarmFresh farmer account has been approved. You can now list your products.");

        log.info("Farmer id={} approved", farmerId);
        return mapToFarmerSummary(farmer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REJECT FARMER
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public DashboardStatsResponse.FarmerSummary rejectFarmer(Long farmerId) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + farmerId));

        if ("REJECTED".equals(farmer.getApprovalStatus())) {
            throw new ValidationException("Farmer is already rejected");
        }

        farmer.setApprovalStatus("REJECTED");
        farmer = farmerRepository.save(farmer);

        sendNotificationToUser(farmer.getUser(),
                "Application Rejected",
                "Your FarmFresh farmer application has been reviewed and was not approved at this time. Please contact support for more information.");

        log.info("Farmer id={} rejected", farmerId);
        return mapToFarmerSummary(farmer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ALL ORDERS (Admin view)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc()
                .stream()
                .map(order -> OrderResponse.builder()
                        .id(order.getId())
                        .orderCode(order.getOrderCode())
                        .orderStatus(order.getOrderStatus())
                        .paymentStatus(order.getPaymentStatus())
                        .totalAmount(order.getTotalAmount())
                        .discountAmount(order.getDiscountAmount())
                        .couponCode(order.getCoupon() != null ? order.getCoupon().getCode() : null)
                        .orderDate(order.getOrderDate())
                        .buyerName(order.getBuyer().getUser().getFullName())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PLATFORM STATISTICS SUMMARY
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalBuyers",         buyerRepository.count());
        stats.put("totalFarmers",        farmerRepository.count());
        stats.put("totalProducts",       productRepository.count());
        stats.put("totalOrders",         orderRepository.count());
        stats.put("totalRevenue",        orderRepository.sumTotalAmountByPaymentStatus("PAID"));
        stats.put("totalReviews",        reviewRepository.count());
        stats.put("activeSubscriptions", subscriptionRepository.countByActiveTrue());
        stats.put("activeAgents",        deliveryAgentRepository.countByActiveTrue());
        return stats;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INTERNAL — Chart builders
    // ─────────────────────────────────────────────────────────────────────────

    private List<Map<String, Object>> buildRevenueByMonth() {
        // Last 6 months
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate month      = now.minusMonths(i);
            LocalDateTime start  = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end    = month.withDayOfMonth(month.lengthOfMonth()).atTime(23, 59, 59);
            BigDecimal rev       = orderRepository.sumTotalAmountByPaymentStatusAndDateBetween("PAID", start, end);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month",   month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + month.getYear());
            entry.put("revenue", rev != null ? rev : BigDecimal.ZERO);
            result.add(entry);
        }
        return result;
    }

    private List<Map<String, Object>> buildOrdersByMonth() {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate month     = now.minusMonths(i);
            LocalDateTime start = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end   = month.withDayOfMonth(month.lengthOfMonth()).atTime(23, 59, 59);
            long count          = orderRepository.countByOrderDateBetween(start, end);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month",  month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + month.getYear());
            entry.put("orders", count);
            result.add(entry);
        }
        return result;
    }

    private List<Map<String, Object>> buildTopProducts() {
        return productRepository.findTop5ByOrderBySoldCountDesc()
                .stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name",          p.getName());
                    m.put("soldCount",     p.getSoldCount());
                    m.put("averageRating", p.getAverageRating());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTopFarmers() {
        return farmerRepository.findTop5ByOrderByAverageRatingDesc()
                .stream()
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name",          f.getUser().getFullName());
                    m.put("farmName",      f.getFarmName());
                    m.put("averageRating", f.getAverageRating());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private void sendNotificationToUser(User user, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    private DashboardStatsResponse.FarmerSummary mapToFarmerSummary(Farmer f) {
        return DashboardStatsResponse.FarmerSummary.builder()
                .farmerId(f.getId())
                .farmerName(f.getUser().getFullName())
                .email(f.getUser().getEmail())
                .phone(f.getUser().getPhone())
                .farmName(f.getFarmName())
                .farmLocation(f.getFarmLocation())
                .approvalStatus(f.getApprovalStatus())
                .registeredDate(f.getUser().getCreatedAt() != null
                        ? f.getUser().getCreatedAt().toString() : "")
                .build();
    }
}
