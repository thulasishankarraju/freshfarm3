package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.ReviewRequest;
import com.example.freshfarm3.dto.response.ReviewResponse;
import com.example.freshfarm3.security.AppUserDetails;
import com.example.freshfarm3.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * POST /api/reviews
     * Buyer submits a review for a product from a delivered order.
     */
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal AppUserDetails userDetails) {

        Long buyerUserId = userDetails.getUserId();
        log.info("POST /api/reviews — buyerUserId={}", buyerUserId);
        ReviewResponse response = reviewService.createReview(request, buyerUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/reviews/product/{productId}
     * Public — anyone can view product reviews.
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByProduct(@PathVariable Long productId) {
        log.info("GET /api/reviews/product/{}", productId);
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    /**
     * GET /api/reviews/product/{productId}/summary
     * Returns average rating + distribution for a product.
     */
    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ReviewResponse.RatingSummary> getProductRatingSummary(@PathVariable Long productId) {
        log.info("GET /api/reviews/product/{}/summary", productId);
        return ResponseEntity.ok(reviewService.getRatingSummaryByProduct(productId));
    }

    /**
     * GET /api/reviews/farmer/{farmerId}
     * Farmers and buyers can view farmer reviews.
     */
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByFarmer(@PathVariable Long farmerId) {
        log.info("GET /api/reviews/farmer/{}", farmerId);
        return ResponseEntity.ok(reviewService.getReviewsByFarmer(farmerId));
    }

    /**
     * GET /api/reviews/my-reviews
     * Logged-in buyer sees their own reviews.
     */
    @GetMapping("/my-reviews")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal AppUserDetails userDetails) {

        Long buyerUserId = userDetails.getUserId();
        log.info("GET /api/reviews/my-reviews — buyerUserId={}", buyerUserId);
        return ResponseEntity.ok(reviewService.getMyReviews(buyerUserId));
    }
}