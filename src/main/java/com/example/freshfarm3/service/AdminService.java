package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.response.DashboardStatsResponse;
import com.example.freshfarm3.dto.response.OrderResponse;
import com.example.freshfarm3.entity.*;
import com.example.freshfarm3.enums.OrderStatus;
import com.example.freshfarm3.exception.ResourceNotFoundException;
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

    private final ShopRepository shopRepository;
    private final BuyerRepository buyerRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final ReviewRepository reviewRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    // ===========================================================
    // Dashboard
    // ===========================================================

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {

        LocalDate today = LocalDate.now();

        long totalUsers = userRepository.count();
        long totalBuyers = buyerRepository.count();
        long totalShops = shopRepository.count();

        long approvedShops = shopRepository.countByApproved(true);
        long pendingShops = totalShops - approvedShops;

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
                .totalShops(totalShops)
                .approvedShops(approvedShops)
                .pendingShops(pendingShops)
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
                .topShops(buildTopShops())
                .build();
    }

    private List<Map<String, Object>> buildRevenueByMonth() {
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(5).withDayOfMonth(1);

        Map<String, BigDecimal> revenueByMonthKey = initLastSixMonths(BigDecimal.ZERO);

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

        Map<String, Long> ordersByMonthKey = initLastSixMonths(0L);

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

    // Shared by buildRevenueByMonth/buildOrdersByMonth — builds the last 6
    // calendar months (oldest first) pre-seeded with a zero value, so months
    // with no orders still show up in the chart instead of being skipped.
    private <T> Map<String, T> initLastSixMonths(T zeroValue) {
        Map<String, T> byMonthKey = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            byMonthKey.put(monthKey(monthStart), zeroValue);
        }
        return byMonthKey;
    }

    private String monthKey(LocalDate monthStart) {
        return monthStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + monthStart.getYear();
    }

    private List<Map<String, Object>> buildTopProducts() {
        Map<Product, BigDecimal> revenueByProduct = new HashMap<>();
        // Quantity sold, kept as BigDecimal (not long) — products are sold
        // in fractional kg/L amounts (e.g. 0.25 kg steps), so truncating to
        // a whole number here would silently understate/misreport sales.
        Map<Product, BigDecimal> unitsByProduct = new HashMap<>();

        orderRepository.findAll().forEach(order -> {
            if (order.getItems() == null) return;
            order.getItems().forEach(item -> {
                Product product = item.getProduct();
                revenueByProduct.merge(product, item.getSubtotal(), BigDecimal::add);
                unitsByProduct.merge(product, item.getQuantity(), BigDecimal::add);
            });
        });

        return revenueByProduct.entrySet().stream()
                .sorted(Map.Entry.<Product, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Product product = entry.getKey();
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", product.getName());
                    row.put("sales", unitsByProduct.getOrDefault(product, BigDecimal.ZERO));
                    row.put("revenue", entry.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTopShops() {
        Map<Shop, List<Review>> reviewsByShop = reviewRepository.findAll().stream()
                .collect(Collectors.groupingBy(Review::getShop));

        Map<Shop, BigDecimal> revenueByShop = new HashMap<>();
        orderRepository.findAll().forEach(order -> {
            if (order.getItems() == null) return;
            order.getItems().forEach(item -> {
                Shop shop = item.getProduct().getShop();
                revenueByShop.merge(shop, item.getSubtotal(), BigDecimal::add);
            });
        });

        return reviewsByShop.entrySet().stream()
                .map(entry -> {
                    Shop shop = entry.getKey();
                    double avgRating = entry.getValue().stream()
                            .mapToInt(Review::getRating)
                            .average()
                            .orElse(0.0);

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", shop.getShopName());
                    row.put("rating", Math.round(avgRating * 10.0) / 10.0);
                    row.put("totalSales", revenueByShop.getOrDefault(shop, BigDecimal.ZERO));
                    return row;
                })
                .sorted((a, b) -> Double.compare((double) b.get("rating"), (double) a.get("rating")))
                .limit(5)
                .collect(Collectors.toList());
    }

    // ===========================================================
    // Pending Shops
    // ===========================================================

    @Transactional(readOnly = true)
    public List<DashboardStatsResponse.ShopSummary> getPendingShops() {
        return shopRepository.findByApproved(false)
                .stream()
                .map(this::mapToShopSummary)
                .collect(Collectors.toList());
    }

    private DashboardStatsResponse.ShopSummary mapToShopSummary(Shop shop) {
        User user = shop.getUser();

        String location = String.join(", ",
                Optional.ofNullable(shop.getVillage()).orElse(""),
                Optional.ofNullable(shop.getDistrict()).orElse(""),
                Optional.ofNullable(shop.getState()).orElse("")
        ).replaceAll("(, )+", ", ").replaceAll("^, |, $", "");

        return DashboardStatsResponse.ShopSummary.builder()
                .shopId(shop.getId())
                .ownerName(user != null ? user.getFullName() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .shopName(shop.getShopName())
                .shopLocation(location)
                .approvalStatus(shop.isApproved() ? "APPROVED" : "PENDING")
                .registeredDate(shop.getCreatedAt() != null
                        ? shop.getCreatedAt().toLocalDate().toString()
                        : null)
                .build();
    }

    @Transactional
    public DashboardStatsResponse.ShopSummary approveShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

        shop.setApproved(true);
        Shop saved = shopRepository.save(shop);

        return mapToShopSummary(saved);
    }

    @Transactional
    public DashboardStatsResponse.ShopSummary rejectShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

        shop.setApproved(false);
        Shop saved = shopRepository.save(shop);

        return mapToShopSummary(saved);
    }

    // ===========================================================
    // Orders
    // ===========================================================

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    // There was previously no way for anyone (admin or shop) to move
    // an order out of PENDING. That silently blocked delivery assignment
    // too, since DeliveryService.assignDelivery expects CONFIRMED/PROCESSING.
    @Transactional
    public OrderResponse confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new RuntimeException(
                    "Only PENDING orders can be confirmed. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);

        // Let the buyer know their order was confirmed.
        notificationService.notifyOrderConfirmed(saved);
        // Let each shop involved know they should pack the order.
        notificationService.notifyShopsOrderConfirmed(saved, saved.getOrderItems());

        return mapToOrderResponse(saved);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        Buyer buyer = order.getBuyer();
        User buyerUser = buyer != null ? buyer.getUser() : null;

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .deliveryCharge(order.getDeliveryCharge())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .buyerId(buyer != null ? buyer.getId() : null)
                .buyerName(buyerUser != null ? buyerUser.getFullName() : null)
                .build();
    }

    // ===========================================================
    // Platform Statistics
    // ===========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformStatistics() {
        long totalUsers = userRepository.count();
        long totalBuyers = buyerRepository.count();
        long totalShops = shopRepository.count();
        long approvedShops = shopRepository.countByApproved(true);
        long pendingShops = totalShops - approvedShops;

        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();

        BigDecimal totalRevenue = orderRepository.findAll()
                .stream()
                .filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeAgents = deliveryAgentRepository.findByIsAvailableTrue().size();
        long totalReviews = reviewRepository.count();
        long activeSubscriptions = subscriptionRepository.count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalBuyers", totalBuyers);
        stats.put("totalShops", totalShops);
        stats.put("approvedShops", approvedShops);
        stats.put("pendingShops", pendingShops);
        stats.put("totalProducts", totalProducts);
        stats.put("totalOrders", totalOrders);
        stats.put("totalRevenue", totalRevenue);
        stats.put("activeDeliveryAgents", activeAgents);
        stats.put("totalReviews", totalReviews);
        stats.put("activeSubscriptions", activeSubscriptions);
        return stats;
    }
}