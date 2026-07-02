package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.PaymentRequest;
import com.example.freshfarm3.dto.request.PaymentVerifyRequest;
import com.example.freshfarm3.dto.response.PaymentResponse;
import com.example.freshfarm3.entity.Order;
import com.example.freshfarm3.entity.OrderItem;
import com.example.freshfarm3.entity.Payment;
import com.example.freshfarm3.enums.OrderStatus;
import com.example.freshfarm3.enums.PaymentMethod;
import com.example.freshfarm3.enums.PaymentStatus;
import com.example.freshfarm3.repository.OrderItemRepository;
import com.example.freshfarm3.repository.OrderRepository;
import com.example.freshfarm3.repository.PaymentRepository;
import com.example.freshfarm3.repository.ProductRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClient       razorpayClient;
    private final PaymentRepository    paymentRepository;
    private final OrderRepository      orderRepository;
    private final OrderItemRepository  orderItemRepository;
    private final ProductRepository    productRepository;
    private final NotificationService  notificationService;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    // ── CREATE RAZORPAY ORDER ─────────────────────────────────────
    @Transactional
    public PaymentResponse createRazorpayOrder(String buyerEmail, PaymentRequest req) {

        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + req.getOrderId()));

        // Verify buyer owns this order
        if (!order.getBuyer().getUser().getEmail().equals(buyerEmail)) {
            throw new RuntimeException("Unauthorized: this order does not belong to you");
        }

        // Check if payment already exists
        if (paymentRepository.findByOrder_Id(order.getId()).isPresent()) {
            throw new RuntimeException("Payment already initiated for order: " + order.getOrderNumber());
        }

        try {
            // Amount in paise (1 INR = 100 paise)
            long amountInPaise = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            options.put("receipt", order.getOrderNumber());
            options.put("notes", new JSONObject().put("farmfresh_order_id", order.getId()));

            com.razorpay.Order rzpOrder = razorpayClient.orders.create(options);
            String rzpOrderId = rzpOrder.get("id");

            // Persist payment in CREATED state
            Payment payment = Payment.builder()
                    .order(order)
                    .razorpayOrderId(rzpOrderId)
                    .paymentMethod(PaymentMethod.UPI)   // default; updated after verification
                    .paymentStatus(PaymentStatus.CREATED)
                    .amount(order.getTotalAmount())
                    .currency("INR")
                    .gateway("RAZORPAY")
                    .build();

            Payment saved = paymentRepository.save(payment);

            // Update order payment status
            order.setPaymentStatus("CREATED");
            orderRepository.save(order);

            log.info("Razorpay order created: {} for FarmFresh order: {}", rzpOrderId, order.getOrderNumber());
            return mapToResponse(saved);

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new RuntimeException("Payment gateway error: " + e.getMessage());
        }
    }

    // ── VERIFY PAYMENT ────────────────────────────────────────────
    @Transactional
    public PaymentResponse verifyPayment(String buyerEmail, PaymentVerifyRequest req) {

        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getBuyer().getUser().getEmail().equals(buyerEmail)) {
            throw new RuntimeException("Unauthorized");
        }

        Payment payment = paymentRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new RuntimeException("Payment record not found for this order"));

        // ── Verify HMAC-SHA256 signature ──────────────────────
        boolean isValid = verifySignature(
                req.getRazorpayOrderId(),
                req.getRazorpayPaymentId(),
                req.getRazorpaySignature()
        );

        if (!isValid) {
            // Signature invalid — mark payment failed, restore stock
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
            paymentRepository.save(payment);

            order.setPaymentStatus("FAILED");
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            // Restore inventory
            restoreInventory(order);

            notificationService.notifyOrderCancelled(order);
            log.warn("Payment verification FAILED for order: {}", order.getOrderNumber());
            throw new RuntimeException("Payment verification failed. Order has been cancelled and inventory restored.");
        }

        // ── Signature valid — confirm payment ─────────────────
        payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
        payment.setRazorpaySignature(req.getRazorpaySignature());
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setTransactionReference(req.getRazorpayPaymentId());
        paymentRepository.save(payment);

        // Update order status
        order.setPaymentStatus("PAID");
        order.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Notify buyer
        notificationService.notifyOrderConfirmed(order);

        log.info("Payment verified successfully for order: {}", order.getOrderNumber());
        return mapToResponse(payment);
    }

    // ── GET PAYMENT BY ORDER ──────────────────────────────────────
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrder(Long orderId) {
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
        return mapToResponse(payment);
    }

    // ── REFUND ────────────────────────────────────────────────────
    @Transactional
    public PaymentResponse processRefund(Long orderId) {
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new RuntimeException("Only successful payments can be refunded");
        }

        try {
            long amountInPaise = payment.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            JSONObject refundOptions = new JSONObject();
            refundOptions.put("amount", amountInPaise);
            refundOptions.put("notes", new JSONObject().put("reason", "Order cancellation refund"));

            com.razorpay.Refund refund = razorpayClient.payments.refund(
                    payment.getRazorpayPaymentId(), refundOptions
            );

            payment.setRefundId(refund.get("id"));
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            // Update order
            Order order = payment.getOrder();
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus("REFUNDED");
            orderRepository.save(order);

            log.info("Refund processed: {} for order: {}", payment.getRefundId(), order.getOrderNumber());
            return mapToResponse(payment);

        } catch (RazorpayException e) {
            log.error("Refund failed: {}", e.getMessage());
            throw new RuntimeException("Refund processing failed: " + e.getMessage());
        }
    }

    // ── VERIFY SIGNATURE (HMAC-SHA256) ────────────────────────────
    public boolean verifySignature(String razorpayOrderId,
                                   String razorpayPaymentId,
                                   String signature) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            ));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    // ── RESTORE INVENTORY on payment failure ──────────────────────
    private void restoreInventory(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        items.forEach(oi -> {
            oi.getProduct().restoreStock(oi.getQuantity());
            productRepository.save(oi.getProduct());
        });
        log.info("Inventory restored for order: {}", order.getOrderNumber());
    }

    // ── RESPONSE MAPPER ───────────────────────────────────────────
    private PaymentResponse mapToResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getId())
                .orderId(p.getOrder().getId())
                .orderNumber(p.getOrder().getOrderNumber())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .paymentMethod(p.getPaymentMethod())
                .paymentStatus(p.getPaymentStatus())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paymentDate(p.getPaymentDate())
                .transactionReference(p.getTransactionReference())
                .gateway(p.getGateway())
                .refundId(p.getRefundId())
                .build();
    }
}
