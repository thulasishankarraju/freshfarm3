package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.AgentReviewRequest;
import com.example.freshfarm3.dto.response.AgentReviewResponse;
import com.example.freshfarm3.entity.*;
import com.example.freshfarm3.enums.OrderStatus;
import com.example.freshfarm3.exception.ResourceNotFoundException;
import com.example.freshfarm3.exception.UnauthorizedException;
import com.example.freshfarm3.exception.ValidationException;
import com.example.freshfarm3.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AgentReviewService — buyer rates the delivery agent (1–5 stars) after
 * their order is DELIVERED. Mirrors ReviewService's product/shop review
 * flow, but the "subject" being reviewed is the agent assigned via
 * Delivery, not a product.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentReviewService {

    private final AgentReviewRepository agentReviewRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final BuyerRepository buyerRepository;
    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;

    // ── CREATE REVIEW ────────────────────────────────────────────
    @Transactional
    public AgentReviewResponse createReview(AgentReviewRequest request, Long buyerUserId) {
        log.info("Creating agent review for orderId={} by buyerUserId={}", request.getOrderId(), buyerUserId);

        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile not found"));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new UnauthorizedException("This order does not belong to you");
        }

        if (!OrderStatus.DELIVERED.equals(order.getOrderStatus())) {
            throw new ValidationException("You can only rate the delivery agent after your order is delivered");
        }

        Delivery delivery = deliveryRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No delivery record found for this order"));

        DeliveryAgent agent = delivery.getDeliveryAgent();
        if (agent == null) {
            throw new ValidationException("No delivery agent was assigned to this order");
        }

        if (agentReviewRepository.existsByOrderIdAndBuyerId(order.getId(), buyer.getId())) {
            throw new ValidationException("You have already rated the delivery agent for this order");
        }

        AgentReview review = AgentReview.builder()
                .agent(agent)
                .buyer(buyer)
                .order(order)
                .rating(request.getRating())
                .reviewComment(request.getReviewComment())
                .build();

        review = agentReviewRepository.save(review);
        log.info("Agent review saved with id={}", review.getId());

        updateAgentRating(agent.getId());

        return mapToResponse(review);
    }

    // ── GET REVIEWS FOR AN AGENT ────────────────────────────────
    @Transactional(readOnly = true)
    public List<AgentReviewResponse> getReviewsByAgent(Long agentId) {
        if (!deliveryAgentRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("Delivery agent not found with id: " + agentId);
        }
        return agentReviewRepository.findByAgentIdOrderByReviewDateDesc(agentId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── GET MY REVIEWS (logged-in buyer) ────────────────────────
    @Transactional(readOnly = true)
    public List<AgentReviewResponse> getMyReviews(Long buyerUserId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile not found"));
        return agentReviewRepository.findByBuyerIdOrderByReviewDateDesc(buyer.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── RATING SUMMARY FOR AN AGENT ─────────────────────────────
    @Transactional(readOnly = true)
    public AgentReviewResponse.RatingSummary getRatingSummaryByAgent(Long agentId) {
        Double avg = agentReviewRepository.calculateAverageRatingByAgent(agentId);
        long total = agentReviewRepository.countByAgentId(agentId);
        return AgentReviewResponse.RatingSummary.builder()
                .averageRating(avg)
                .totalReviews(total)
                .build();
    }

    // ── INTERNAL — recalc + persist agent average rating ────────
    @Transactional
    public void updateAgentRating(Long agentId) {
        Double avg = agentReviewRepository.calculateAverageRatingByAgent(agentId);
        long count = agentReviewRepository.countByAgentId(agentId);
        deliveryAgentRepository.findById(agentId).ifPresent(a -> {
            a.setAverageRating(avg != null ? avg : 0.0);
            a.setReviewCount((int) count);
            deliveryAgentRepository.save(a);
            log.info("Updated agent id={} averageRating={} reviewCount={}", agentId, avg, count);
        });
    }

    // ── MAPPER ───────────────────────────────────────────────────
    private AgentReviewResponse mapToResponse(AgentReview r) {
        return AgentReviewResponse.builder()
                .id(r.getId())
                .agentId(r.getAgent().getId())
                .agentName(r.getAgent().getUser().getFullName())
                .buyerId(r.getBuyer().getId())
                .buyerName(r.getBuyer().getUser().getFullName())
                .orderId(r.getOrder().getId())
                .orderNumber(r.getOrder().getOrderNumber())
                .rating(r.getRating())
                .reviewComment(r.getReviewComment())
                .reviewDate(r.getReviewDate())
                .build();
    }
}
