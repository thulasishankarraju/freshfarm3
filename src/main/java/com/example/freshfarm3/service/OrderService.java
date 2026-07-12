package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.CheckoutRequest;
import com.example.freshfarm3.dto.response.CheckoutResponse;
import com.example.freshfarm3.entity.*;
import com.example.freshfarm3.repository.*;
import com.example.freshfarm3.enums.OrderStatus;
import com.example.freshfarm3.util.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BuyerRepository buyerRepository;
    private final AddressRepository addressRepository;
    private final CartService            cartService;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final NotificationRepository notificationRepository;
    private final OrderNumberGenerator   orderNumberGenerator;

    private static final BigDecimal DELIVERY_CHARGE = BigDecimal.valueOf(50);
    private static final BigDecimal FREE_DELIVERY_ABOVE = BigDecimal.valueOf(500);

    // ── PLACE ORDER ───────────────────────────────────────────────
    @Transactional
    public CheckoutResponse placeOrder(String buyerEmail, CheckoutRequest req) {

        // 1. Load buyer
        Buyer buyer = buyerRepository.findByUserEmail(buyerEmail)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));

        // 2. Load delivery address and verify ownership
        Address address = addressRepository.findById(req.getAddressId())
                .orElseThrow(() -> new RuntimeException("Address not found"));
        if (!address.getBuyer().getId().equals(buyer.getId())) {
            throw new RuntimeException("Address does not belong to this buyer");
        }

        // 3. Load cart and validate it's not empty
        Cart cart = cartService.getCartEntity(buyerEmail);
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cannot place order with an empty cart");
        }

        // 4. Validate stock for every item in the cart
        for (CartItem ci : cartItems) {
            Product product = ci.getProduct();
            if (!Boolean.TRUE.equals(product.getIsAvailable())) {
                throw new RuntimeException(
                        "Product '" + product.getName() + "' is no longer available"
                );
            }
            if (!product.hasStock(ci.getQuantity())) {
                throw new RuntimeException(
                        "Insufficient stock for '" + product.getName() +
                                "'. Available: " + product.getStockQuantity() +
                                ", In cart: " + ci.getQuantity()
                );
            }
        }

        // 5. Calculate subtotal
        BigDecimal subtotal = cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. Calculate delivery charge (free above ₹500)
        BigDecimal deliveryCharge = subtotal.compareTo(FREE_DELIVERY_ABOVE) >= 0
                ? BigDecimal.ZERO
                : DELIVERY_CHARGE;

        BigDecimal totalAmount = subtotal.add(deliveryCharge);

        // 7. Generate order number
        String orderNumber = orderNumberGenerator.generate();

        // 8. Create Order entity
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .buyer(buyer)
                .deliveryAddress(address)
                .subtotal(subtotal)
                .deliveryCharge(deliveryCharge)
                .totalAmount(totalAmount)
                .paymentStatus("PENDING")
                .orderStatus(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .otpChannel(normalizeOtpChannel(req.getOtpChannel()))
                .items(new ArrayList<>())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 9. Create OrderItems and deduct stock
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cartItems) {
            Product product = ci.getProduct();

            OrderItem oi = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(ci.getQuantity())
                    .price(product.getPrice())       // snapshot price at order time
                    .subtotal(ci.getSubtotal())
                    .build();
            orderItems.add(oi);

            // Deduct stock — entity method prevents negative stock
            product.deductStock(ci.getQuantity());
            productRepository.save(product);
        }
        orderItemRepository.saveAll(orderItems);

        // 10. Save notifications (buyer + farmers)
        saveOrderNotifications(savedOrder, buyer, cartItems);

        // 11. Clear cart
        cartItemRepository.deleteAll(cartItems);

        log.info("Order placed: {} for buyer: {}", orderNumber, buyerEmail);

        // 12. Build and return response
        return buildCheckoutResponse(savedOrder, orderItems, address);
    }

    // ── CANCEL ORDER ──────────────────────────────────────────────
    @Transactional
    public CheckoutResponse cancelOrder(String buyerEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getBuyer().getUser().getEmail().equals(buyerEmail)) {
            throw new RuntimeException("Unauthorized: this order does not belong to you");
        }

        if (order.getOrderStatus() == OrderStatus.SHIPPED ||
                order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException(
                    "Cannot cancel order that is already " + order.getOrderStatus()
            );
        }

        // Restore stock for all items
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem oi : items) {
            oi.getProduct().restoreStock(oi.getQuantity());
            productRepository.save(oi.getProduct());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Save cancellation notification
        Notification notif = new Notification();
        notif.setUser(order.getBuyer().getUser());
        notif.setTitle("Order Cancelled");
        notif.setMessage("Your order #" + order.getOrderNumber() + " has been cancelled.");
        notif.setIsRead(false);
        notificationRepository.save(notif);

        log.info("Order cancelled: {} by buyer: {}", order.getOrderNumber(), buyerEmail);
        return buildCheckoutResponse(order, items, order.getDeliveryAddress());
    }

    // ── VIEW MY ORDERS ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CheckoutResponse> getMyOrders(String buyerEmail) {
        Buyer buyer = buyerRepository.findByUserEmail(buyerEmail)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));

        return orderRepository.findByBuyerOrderByOrderDateDesc(buyer).stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrder(order);
                    return buildCheckoutResponse(order, items, order.getDeliveryAddress());
                })
                .collect(Collectors.toList());
    }

    // ── VIEW SINGLE ORDER ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public CheckoutResponse getOrderById(String buyerEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getBuyer().getUser().getEmail().equals(buyerEmail)) {
            throw new RuntimeException("Unauthorized: this order does not belong to you");
        }

        List<OrderItem> items = orderItemRepository.findByOrder(order);
        return buildCheckoutResponse(order, items, order.getDeliveryAddress());
    }

    // ── FARMER: VIEW ORDERS FOR MY PRODUCTS ───────────────────────
    @Transactional(readOnly = true)
    public List<CheckoutResponse> getOrdersForFarmer(String farmerEmail) {
        return orderRepository.findByItems_Product_Farmer_User_Email(farmerEmail).stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrder(order);
                    return buildCheckoutResponse(order, items, order.getDeliveryAddress());
                })
                .collect(Collectors.toList());
    }

    // ── ADMIN: VIEW ALL ORDERS ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CheckoutResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrder(order);
                    return buildCheckoutResponse(order, items, order.getDeliveryAddress());
                })
                .collect(Collectors.toList());
    }

    // ── NOTIFICATIONS ─────────────────────────────────────────────
    private String normalizeOtpChannel(String requested) {
        if (requested == null) return "BOTH";
        String upper = requested.trim().toUpperCase();
        return switch (upper) {
            case "EMAIL", "PHONE", "BOTH" -> upper;
            default -> "BOTH";
        };
    }

    private void saveOrderNotifications(Order order, Buyer buyer, List<CartItem> cartItems) {
        // Notify buyer
        Notification buyerNotif = new Notification();
        buyerNotif.setUser(buyer.getUser());
        buyerNotif.setTitle("Order Placed Successfully 🌿");
        buyerNotif.setMessage("Your order #" + order.getOrderNumber() +
                " has been placed for ₹" + order.getTotalAmount() + ". We'll notify you when confirmed.");
        buyerNotif.setIsRead(false);
        notificationRepository.save(buyerNotif);

        // Notify each unique farmer who has a product in this order
        cartItems.stream()
                .map(ci -> ci.getProduct().getFarmer())
                .distinct()
                .forEach(farmer -> {
                    Notification farmerNotif = new Notification();
                    farmerNotif.setUser(farmer.getUser());
                    farmerNotif.setTitle("New Order Received! 🧑‍🌾");
                    farmerNotif.setMessage("You have a new order #" + order.getOrderNumber() +
                            " from " + buyer.getUser().getFullName() + ". Please confirm it.");
                    farmerNotif.setIsRead(false);
                    notificationRepository.save(farmerNotif);
                });
    }

    // ── RESPONSE BUILDER ──────────────────────────────────────────
    private CheckoutResponse buildCheckoutResponse(Order order,
                                                   List<OrderItem> items,
                                                   Address address) {
        List<CheckoutResponse.OrderItemDto> itemDtos = items.stream()
                .map(oi -> CheckoutResponse.OrderItemDto.builder()
                        .productId(oi.getProduct().getId())
                        .productName(oi.getProduct().getName())
                        .quantity(oi.getQuantity())
                        .pricePerUnit(oi.getPrice())
                        .subtotal(oi.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        String addressStr = address.getAddressLine() + ", " + address.getCity() +
                ", " + address.getState() + " - " + address.getPincode();

        return CheckoutResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .deliveryCharge(order.getDeliveryCharge())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .deliveryAddress(addressStr)
                .items(itemDtos)
                .build();
    }
}