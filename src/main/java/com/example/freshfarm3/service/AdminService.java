package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.response.DashboardStatsResponse;
import com.example.freshfarm3.entity.*;
import com.example.freshfarm3.enums.OrderStatus;
import com.example.freshfarm3.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final ReviewRepository reviewRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;

    // ===========================================================
    // Dashboard
    // ===========================================================

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {

        LocalDate today = LocalDate.now();

        long totalUsers = userRepository.count();
        long totalBuyers = buyerRepository.count();
        long totalFarmers = farmerRepository.count();

        long approvedFarmers = farmerRepository.countByApproved(true);
        long pendingFarmers = totalFarmers - approvedFarmers;

        long totalProducts = productRepository.count();

        long activeProducts = productRepository.findAll()
                .stream()
                .filter(Product::getIsAvailable)
                .count();

        long totalOrders = orderRepository.count();

        long deliveredOrders = orderRepository.findByOrderStatus(OrderStatus.DELIVERED).size();
        long shippedOrders = orderRepository.findByOrderStatus(OrderStatus.SHIPPED).size();

        long ordersToday = orderRepository.findAll()
                .stream()
                .filter(o -> o.getOrderDate() != null)
                .filter(o -> o.getOrderDate().toLocalDate().equals(today))
                .count();

        BigDecimal revenueToday = orderRepository.findAll()
                .stream()
                .filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus()))
                .filter(o -> o.getOrderDate() != null)
                .filter(o -> o.getOrderDate().toLocalDate().equals(today))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = orderRepository.findAll()
                .stream()
                .filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeAgents = deliveryAgentRepository.findByIsAvailableTrue().size();
        long totalReviews = reviewRepository.count();
        long activeSubscriptions = subscriptionRepository.count();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalBuyers(totalBuyers)
                .totalFarmers(totalFarmers)
                .approvedFarmers(approvedFarmers)
                .pendingFarmers(pendingFarmers)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .totalOrders(totalOrders)
                .ordersToday(ordersToday)
                .ordersThisMonth(0)
                .deliveredOrders(deliveredOrders)
                .pendingDeliveries(shippedOrders)
                .totalRevenue(totalRevenue)
                .revenueToday(revenueToday)
                .revenueThisMonth(BigDecimal.ZERO)
                .averageProductRating(0)
                .activeDeliveryAgents(activeAgents)
                .totalReviews(totalReviews)
                .activeSubscriptions(activeSubscriptions)
                .revenueByMonth(buildRevenueByMonth())
                .ordersByMonth(buildOrdersByMonth())
                .topProducts(buildTopProducts())
                .topFarmers(buildTopFarmers())
                .build();
    }

    private List<Map<String, Object>> buildRevenueByMonth() {
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(5).withDayOfMonth(1);

        Map<String, BigDecimal> revenueByMonthKey = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            revenueByMonthKey.put(monthKey(monthStart), BigDecimal.ZERO);
        }

        orderRepository.findAll().stream()
                .filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus()))
                .filter(o -> o.getOrderDate() != null)
                .filter(o -> !o.getOrderDate().toLocalDate().isBefore(sixMonthsAgo))
                .forEach(o -> {
                    String key = monthKey(o.getOrderDate().toLocalDate().withDayOfMonth(1));
                    revenueByMonthKey.merge(key, o.getTotalAmount(), BigDecimal::add);
                });

        List<Map<String, Object>> result = new ArrayList<>();
        revenueByMonthKey.forEach((month, revenue) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", month);
            entry.put("revenue", revenue);
            result.add(entry);
        });
        return result;
    }

    private List<Map<String, Object>> buildOrdersByMonth() {
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(5).withDayOfMonth(1);

        Map<String, Long> ordersByMonthKey = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            ordersByMonthKey.put(monthKey(monthStart), 0L);
        }

        orderRepository.findAll().stream()
                .filter(o -> o.getOrderDate() != null)
                .filter(o -> !o.getOrderDate().toLocalDate().isBefore(sixMonthsAgo))
                .forEach(o -> {
                    String key = monthKey(o.getOrderDate().toLocalDate().withDayOfMonth(1));
                    ordersByMonthKey.merge(key, 1L, Long::sum);
                });

        List<Map<String, Object>> result = new ArrayList<>();
        ordersByMonthKey.forEach((month, count) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", month);
            entry.put("orders", count);
            result.add(entry);
        });
        return result;
    }

    private String monthKey(LocalDate monthStart) {
        return monthStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + monthStart.getYear();
    }

    private List<Map<String, Object>> buildTopProducts() {
        Map<Product, BigDecimal> revenueByProduct = new HashMap<>();
        Map<Product, Long> unitsByProduct = new HashMap<>();

        orderRepository.findAll().forEach(order -> {
            if (order.getItems() == null) return;
            order.getItems().forEach(item -> {
                Product product = item.getProduct();
                revenueByProduct.merge(product, item.getSubtotal(), BigDecimal::add);
                unitsByProduct.merge(product, (long) item.getQuantity(), Long::sum);
            });
        });

        return revenueByProduct.entrySet().stream()
                .sorted(Map.Entry.<Product, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Product product = entry.getKey();
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", product.getName());
                    row.put("sales", unitsByProduct.getOrDefault(product, 0L));
                    row.put("revenue", entry.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTopFarmers() {
        Object Review = null;
        Map<Farmer, List<Review>> reviewsByFarmer = reviewRepository.findAll().stream()
                .collect(Collectors.groupingBy(Review::getFarmer));

        Map<Farmer, BigDecimal> revenueByFarmer = new HashMap<>();
        orderRepository.findAll().forEach(order -> {
            if (order.getItems() == null) return;
            order.getItems().forEach(item -> {
                Farmer farmer = item.getProduct().getFarmer();
                revenueByFarmer.merge(farmer, item.getSubtotal(), BigDecimal::add);
            });
        });

        return reviewsByFarmer.entrySet().stream()
                .map(entry -> {
                    Farmer farmer = entry.getKey();
                    double avgRating = entry.getValue().stream(
                            .mapToInt(Review::getRating)
                            .average()
                            .orElse(0.0);

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", farmer.getFarmName());
                    row.put("rating", Math.round(avgRating * 10.0) / 10.0);
                    row.put("totalSales", revenueByFarmer.getOrDefault(farmer, BigDecimal.ZERO));
                    return row;
                })
                .sorted((a, b) -> Double.compare((double) b.get("rating"), (double) a.get("rating")))
                .limit(5)
                .collect(Collectors.toList());
    }

    // ===========================================================
    // Pending Farmers
    // ===========================================================

    @Transactional(readOnly = true)
    public List<DashboardStatsResponse.FarmerSummary> getPendingFarmers() {
        return farmerRepository.findByApproved(false)
                .stream()
                .map(this::mapToFarmerSummary)
                .collect(Collectors.toList());
    }

    private DashboardStatsResponse.FarmerSummary mapToFarmerSummary(Farmer farmer) {
        User user = farmer.getUser();

        String location = String.join(", ",
                Optional.ofNullable(farmer.getVillage()).orElse(""),
                Optional.ofNullable(farmer.getDistrict()).orElse(""),
                Optional.ofNullable(farmer.getState()).orElse("")
        ).replaceAll("(, )+", ", ").replaceAll("^, |, $", "");

        return DashboardStatsResponse.FarmerSummary.builder()
                .farmerId(farmer.getId())
                .farmerName(user != null ? user.getFullName() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .farmName(farmer.getFarmName())
                .farmLocation(location)
                .approvalStatus(farmer.isApproved() ? "APPROVED" : "PENDING")
                .registeredDate(farmer.getCreatedAt() != null
                        ? farmer.getCreatedAt().toLocalDate().toString()
                        : null)
                .build();
    }
}