package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.Farmer;
import com.example.freshfarm3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * FarmerRepository — Database operations for Farmer entity.
 */
@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long> {

    /**
     * Find a Farmer profile by their User account.
     * SELECT * FROM farmers WHERE user_id = ?
     */
    Optional<Farmer> findByUser(User user);

    /**
     * Check if a Farmer profile exists for a given User.
     */
    boolean existsByUser(User user);

    // ── Admin dashboard / approval workflow ──────────────────────
    long countByApproved(boolean approved);

    List<Farmer> findByApproved(boolean approved);

    Optional<Farmer> findByUser_Email(String farmerEmail);

    Arrays findByApprovalStatus(String pending);
}