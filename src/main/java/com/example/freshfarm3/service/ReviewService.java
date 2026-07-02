package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.ReviewRequest;
import com.example.freshfarm3.dto.response.ReviewResponse;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final BuyerRepository buyerRepository;
    private final OrderRepository orderRepository;
    private final FarmerRepository farmerRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE REVIEW
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ReviewResponse createReview(ReviewRequest request, Long buyerUserId) {
        log.info("Creating review for productId={} by buyerUserId={}", request.getProductId(), buyerUserId);

        // 1. Fetch buyer
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile not found"));

        // 2. Fetch product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        // 3. Fetch order and validate it belongs to this buyer
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new UnauthorizedException("This order does not belong to you");
        }

        // 4. Validate order is DELIVERED
        if (!OrderStatus.DELIVERED.equals(order.getOrderStatus())) {
            throw new ValidationException("You can only review products from delivered orders");
        }

        // 5. Validate the ordered product is actually in this order
        boolean productInOrder = order.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(product.getId()));
        if (!productInOrder) {
            throw new ValidationException("This product was not part of the specified order");
        }

        // 6. Prevent duplicate review
        if (reviewRepository.existsByOrderIdAndBuyerId(order.getId(), buyer.getId())) {
            throw new ValidationException("You have already reviewed this order");
        }

        // 7. Build and save review
        Review review = Review.builder()
                .product(product)
                .buyer(buyer)
                .order(order)
                .farmer(product.getFarmer())
                .rating(request.getRating())
                .reviewTitle(request.getReviewTitle())
                .reviewComment(request.getReviewComment())
                .verifiedPurchase(true)
                .build();

        review = reviewRepository.save(review);
        log.info("Review saved with id={}", review.getId());

        // 8. Recalculate product average rating
        updateProductRating(product.getId());

        // 9. Recalculate farmer average rating
        updateFarmerRating(product.getFarmer().getId());

        return mapToResponse(review);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET REVIEWS BY PRODUCT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return reviewRepository.findByProductIdOrderByReviewDateDesc(productId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET REVIEWS BY FARMER
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByFarmer(Long farmerId) {
        if (!farmerRepository.existsById(farmerId)) {
            throw new ResourceNotFoundException("Farmer not found with id: " + farmerId);
        }
        return reviewRepository.findByFarmerIdOrderByReviewDateDesc(farmerId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET MY REVIEWS (logged-in buyer)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(Long buyerUserId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile not found"));
        return reviewRepository.findByBuyerIdOrderByReviewDateDesc(buyer.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RATING SUMMARY FOR A PRODUCT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReviewResponse.RatingSummary getRatingSummaryByProduct(Long productId) {
        Double avg = reviewRepository.calculateAverageRatingByProduct(productId);
        long total = reviewRepository.countByProductId(productId);

        List<Object[]> rows = reviewRepository.getRatingDistributionByProduct(productId);
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            distribution.put(i, 0L);
        }
        for (Object[] row : rows) {
            distribution.put((Integer) row[0], (Long) row[1]);
        }

        return ReviewResponse.RatingSummary.builder()
                .averageRating(avg)
                .totalReviews(total)
                .ratingDistribution(distribution)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INTERNAL — Recalculate and persist product average rating
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void updateProductRating(Long productId) {
        Double avg = reviewRepository.calculateAverageRatingByProduct(productId);
        long count = reviewRepository.countByProductId(productId);
        productRepository.findById(productId).ifPresent(p -> {
            p.setAverageRating(avg != null ? avg : 0.0);
            p.setReviewCount((int) count);
            productRepository.save(p);
            log.info("Updated product id={} averageRating={} reviewCount={}", productId, avg, count);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INTERNAL — Recalculate and persist farmer average rating
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void updateFarmerRating(Long farmerId) {
        Double avg = reviewRepository.calculateAverageRatingByFarmer(farmerId);
        farmerRepository.findById(farmerId).ifPresent(f -> {
            f.setAverageRating(avg != null ? avg : 0.0);
            farmerRepository.save(f);
            log.info("Updated farmer id={} averageRating={}", farmerId, avg);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MAPPER
    // ─────────────────────────────────────────────────────────────────────────

    private ReviewResponse mapToResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .productId(r.getProduct().getId())
                .productName(r.getProduct().getName())
                .buyerId(r.getBuyer().getId())
                .buyerName(r.getBuyer().getUser().getFullName())
                .farmerId(r.getFarmer().getId())
                .farmerName(r.getFarmer().getUser().getFullName())
                .orderId(r.getOrder().getId())
                .rating(r.getRating())
                .reviewTitle(r.getReviewTitle())
                .reviewComment(r.getReviewComment())
                .reviewDate(r.getReviewDate())
                .verifiedPurchase(r.isVerifiedPurchase())
                .build();
    }
}
