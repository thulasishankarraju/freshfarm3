package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Buyer;
import com.example.freshfarm3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * BuyerRepository — Database operations for Buyer entity.
 */
public interface BuyerRepository extends JpaRepository<Buyer, Long> {

    /**
     * Find a Buyer profile by their associated User account.
     * Derived query: SELECT * FROM buyers WHERE user_id = ?
     */
    Optional<Buyer> findByUser(User user);

    /**
     * Check if a Buyer profile exists for a given User account.
     * Derived query: SELECT COUNT(*) > 0 FROM buyers WHERE user_id = ?
     */
    boolean existsByUser(User user);
}