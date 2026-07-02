package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Coupon> findAllByOrderByCreatedAtDesc();

    List<Coupon> findByActiveTrue();

    // Deactivate all coupons whose validUntil is in the past
    @Modifying
    @Transactional
    @Query("UPDATE Coupon c SET c.active = false WHERE c.validUntil < :now AND c.active = true")
    int deactivateExpiredCoupons(@Param("now") LocalDateTime now);
}
