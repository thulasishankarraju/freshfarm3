package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.AgentReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentReviewRepository extends JpaRepository<AgentReview, Long> {

    // All reviews for a given delivery agent
    List<AgentReview> findByAgentIdOrderByReviewDateDesc(Long agentId);

    // All reviews written by a specific buyer
    List<AgentReview> findByBuyerIdOrderByReviewDateDesc(Long buyerId);

    // One agent review per order per buyer
    boolean existsByOrderIdAndBuyerId(Long orderId, Long buyerId);

    // Average rating for an agent
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM AgentReview r WHERE r.agent.id = :agentId")
    Double calculateAverageRatingByAgent(@Param("agentId") Long agentId);

    // Count of reviews for an agent
    long countByAgentId(Long agentId);
}
