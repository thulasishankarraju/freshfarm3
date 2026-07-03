package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Buyer;
import com.example.freshfarm3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * BuyerRepository — Database operations for Buyer entity.
 */
@Repository
public interface BuyerRepository extends JpaRepository<Buyer, Long> {

    /**
     * Find Buyer by User entity.
     */
    Optional<Buyer> findByUser(User user);

    /**
     * Find Buyer using User email.
     */
    Optional<Buyer> findByUser_Email(String email);

    /**
     * Check if Buyer exists for User.
     */
    boolean existsByUser(User user);

    Optional<Buyer> findByUserEmail(String email);
}