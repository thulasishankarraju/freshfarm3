package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.SubscriptionRequest;
import com.example.freshfarm3.dto.response.SubscriptionResponse;
import com.example.freshfarm3.entity.Buyer;
import com.example.freshfarm3.entity.Notification;
import com.example.freshfarm3.entity.Subscription;
import com.example.freshfarm3.entity.User;
import com.example.freshfarm3.repository.BuyerRepository;
import com.example.freshfarm3.repository.ProductRepository;
import com.example.freshfarm3.repository.SubscriptionRepository;
import com.example.freshfarm3.entity.*;
import com.example.freshfarm3.enums.SubscriptionFrequency;
import com.example.freshfarm3.exception.ResourceNotFoundException;
import com.example.freshfarm3.exception.UnauthorizedException;
import com.example.freshfarm3.exception.ValidationException;
import com.example.freshfarm3.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final BuyerRepository buyerRepository;
    private final ProductRepository productRepository;
    private final NotificationRepository notificationRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE SUBSCRIPTION
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public SubscriptionResponse createSubscription(SubscriptionRequest request, Long buyerUserId) {
        log.info("Creating subscription for productId={} buyerUserId={}", request.getProductId(), buyerUserId);

        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        if (!product.isAvailable()) {
            throw new ValidationException("Product is not available for subscription");
        }

        if (subscriptionRepository.existsByBuyerIdAndProductIdAndActiveTrue(buyer.getId(), product.getId())) {
            throw new ValidationException("You already have an active subscription for this product");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new ValidationException("Start date cannot be in the past");
        }

        LocalDate nextDelivery = calculateNextDeliveryDate(request.getStartDate(), request.getFrequency());

        Subscription subscription = Subscription.builder()
                .buyer(buyer)
                .product(product)
                .quantity(request.getQuantity())
                .frequency(request.getFrequency())
                .startDate(request.getStartDate())
                .nextDeliveryDate(nextDelivery)
                .active(true)
                .paused(false)
                .build();

        subscription = subscriptionRepository.save(subscription);

        sendNotification(buyer.getUser(),
                "Subscription Created",
                "Your subscription for " + product.getName() + " has been created. Next delivery: " + nextDelivery);

        log.info("Subscription created id={}", subscription.getId());
        return mapToResponse(subscription);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PAUSE SUBSCRIPTION
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public SubscriptionResponse pauseSubscription(Long subscriptionId, Long buyerUserId) {
        Subscription subscription = getValidatedSubscription(subscriptionId, buyerUserId);

        if (subscription.isPaused()) {
            throw new ValidationException("Subscription is already paused");
        }

        subscription.setPaused(true);
        subscription = subscriptionRepository.save(subscription);

        sendNotification(subscription.getBuyer().getUser(),
                "Subscription Paused",
                "Your subscription for " + subscription.getProduct().getName() + " has been paused");

        log.info("Subscription {} paused", subscriptionId);
        return mapToResponse(subscription);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RESUME SUBSCRIPTION
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public SubscriptionResponse resumeSubscription(Long subscriptionId, Long buyerUserId) {
        Subscription subscription = getValidatedSubscription(subscriptionId, buyerUserId);

        if (!subscription.isPaused()) {
            throw new ValidationException("Subscription is not paused");
        }

        // Recalculate next delivery from today forward
        LocalDate nextDelivery = calculateNextDeliveryDate(LocalDate.now(), subscription.getFrequency());

        subscription.setPaused(false);
        subscription.setNextDeliveryDate(nextDelivery);
        subscription = subscriptionRepository.save(subscription);

        sendNotification(subscription.getBuyer().getUser(),
                "Subscription Resumed",
                "Your subscription for " + subscription.getProduct().getName()
                        + " has been resumed. Next delivery: " + nextDelivery);

        log.info("Subscription {} resumed", subscriptionId);
        return mapToResponse(subscription);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CANCEL SUBSCRIPTION
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void cancelSubscription(Long subscriptionId, Long buyerUserId) {
        Subscription subscription = getValidatedSubscription(subscriptionId, buyerUserId);

        subscription.setActive(false);
        subscriptionRepository.save(subscription);

        sendNotification(subscription.getBuyer().getUser(),
                "Subscription Cancelled",
                "Your subscription for " + subscription.getProduct().getName() + " has been cancelled");

        log.info("Subscription {} cancelled", subscriptionId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET MY SUBSCRIPTIONS
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getMySubscriptions(Long buyerUserId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile not found"));
        return subscriptionRepository.findByBuyerIdOrderByCreatedDateDesc(buyer.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SCHEDULED: Advance next delivery dates daily at 6 AM
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void processSubscriptionDeliveries() {
        List<Subscription> due = subscriptionRepository.findDueSubscriptions(LocalDate.now());
        log.info("Processing {} due subscriptions", due.size());

        for (Subscription sub : due) {
            try {
                // Advance to next cycle
                LocalDate next = calculateNextDeliveryDate(LocalDate.now().plusDays(1), sub.getFrequency());
                sub.setNextDeliveryDate(next);
                subscriptionRepository.save(sub);

                sendNotification(sub.getBuyer().getUser(),
                        "Subscription Delivery Today",
                        "Your subscription delivery for " + sub.getProduct().getName()
                                + " (Qty: " + sub.getQuantity() + ") is scheduled for today!");
            } catch (Exception e) {
                log.error("Failed to process subscription id={}: {}", sub.getId(), e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private LocalDate calculateNextDeliveryDate(LocalDate from, SubscriptionFrequency frequency) {
        return switch (frequency) {
            case DAILY   -> from;
            case WEEKLY  -> from.plusWeeks(1);
            case MONTHLY -> from.plusMonths(1);
        };
    }

    private Subscription getValidatedSubscription(Long subscriptionId, Long buyerUserId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile not found"));

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        if (!subscription.getBuyer().getId().equals(buyer.getId())) {
            throw new UnauthorizedException("This subscription does not belong to you");
        }
        if (!subscription.isActive()) {
            throw new ValidationException("Subscription is already cancelled");
        }
        return subscription;
    }

    private void sendNotification(User user, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    private SubscriptionResponse mapToResponse(Subscription s) {
        String imageUrl = (s.getProduct().getProductImages() != null && !s.getProduct().getProductImages().isEmpty())
                ? s.getProduct().getProductImages().get(0).getImageUrl()
                : null;

        return SubscriptionResponse.builder()
                .id(s.getId())
                .buyerId(s.getBuyer().getId())
                .buyerName(s.getBuyer().getUser().getFullName())
                .productId(s.getProduct().getId())
                .productName(s.getProduct().getName())
                .productImageUrl(imageUrl)
                .quantity(s.getQuantity())
                .frequency(s.getFrequency())
                .startDate(s.getStartDate())
                .nextDeliveryDate(s.getNextDeliveryDate())
                .active(s.isActive())
                .paused(s.isPaused())
                .createdDate(s.getCreatedDate())
                .build();
    }
}
