package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Shop;
import com.example.freshfarm3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ShopRepository — Database operations for Shop entity.
 */
@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    /**
     * Find a Shop profile by their User account.
     * SELECT * FROM shops WHERE user_id = ?
     */
    Optional<Shop> findByUser(User user);

    /**
     * Check if a Shop profile exists for a given User.
     */
    boolean existsByUser(User user);

    // ── Admin dashboard / approval workflow ──────────────────────
    long countByApproved(boolean approved);

    List<Shop> findByApproved(boolean approved);

    Optional<Shop> findByUser_Email(String shopEmail);
}