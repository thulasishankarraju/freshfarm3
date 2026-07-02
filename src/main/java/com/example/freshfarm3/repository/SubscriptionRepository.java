package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // All subscriptions for a buyer
    List<Subscription> findByBuyerIdOrderByCreatedDateDesc(Long buyerId);

    // Active subscriptions due for delivery today or earlier (for scheduler)
    @Query("SELECT s FROM Subscription s WHERE s.active = true AND s.paused = false " +
            "AND s.nextDeliveryDate <= :today")
    List<Subscription> findDueSubscriptions(@Param("today") LocalDate today);

    // Check if buyer already has an active subscription for a product
    boolean existsByBuyerIdAndProductIdAndActiveTrue(Long buyerId, Long productId);

    long countByActiveTrue();
}
