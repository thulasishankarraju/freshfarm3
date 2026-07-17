package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.AgentReviewRequest;
import com.example.freshfarm3.dto.response.AgentReviewResponse;
import com.example.freshfarm3.security.AppUserDetails;
import com.example.freshfarm3.service.AgentReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AgentReviewController — was previously missing entirely: AgentReviewService
 * existed (buyer rates the delivery agent after DELIVERED) but nothing ever
 * called it, so the feature was unreachable via the API.
 */
@RestController
@RequestMapping("/api/reviews/agent")
@RequiredArgsConstructor
@Slf4j
public class AgentReviewController {

    private final AgentReviewService agentReviewService;

    /**
     * POST /api/reviews/agent
     * Buyer rates the delivery agent (1–5 stars) for a DELIVERED order.
     */
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<AgentReviewResponse> createReview(
            @Valid @RequestBody AgentReviewRequest request,
            @AuthenticationPrincipal AppUserDetails userDetails) {

        Long buyerUserId = userDetails.getUserId();
        log.info("POST /api/reviews/agent — buyerUserId={}", buyerUserId);
        AgentReviewResponse response = agentReviewService.createReview(request, buyerUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/reviews/agent/my-reviews
     * Logged-in buyer sees the agent reviews they've written.
     */
    @GetMapping("/my-reviews")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<AgentReviewResponse>> getMyReviews(
            @AuthenticationPrincipal AppUserDetails userDetails) {

        Long buyerUserId = userDetails.getUserId();
        log.info("GET /api/reviews/agent/my-reviews — buyerUserId={}", buyerUserId);
        return ResponseEntity.ok(agentReviewService.getMyReviews(buyerUserId));
    }

    /**
     * GET /api/reviews/agent/{agentId}/summary
     * Average rating + review count for a delivery agent.
     */
    @GetMapping("/{agentId}/summary")
    public ResponseEntity<AgentReviewResponse.RatingSummary> getRatingSummary(@PathVariable Long agentId) {
        log.info("GET /api/reviews/agent/{}/summary", agentId);
        return ResponseEntity.ok(agentReviewService.getRatingSummaryByAgent(agentId));
    }

    /**
     * GET /api/reviews/agent/{agentId}
     * All reviews for a delivery agent.
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<List<AgentReviewResponse>> getReviewsByAgent(@PathVariable Long agentId) {
        log.info("GET /api/reviews/agent/{}", agentId);
        return ResponseEntity.ok(agentReviewService.getReviewsByAgent(agentId));
    }
}