package com.example.freshfarm3.repository;

import com.example.freshfarm3.entity.DeliveryAgent;
import com.example.freshfarm3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, Long> {

    Optional<DeliveryAgent> findByUser(User user);

    Optional<DeliveryAgent> findByUser_Email(String email);

    List<DeliveryAgent> findByIsAvailableTrue();

    boolean existsByUser_Email(String email);

    long countByIsAvailableTrue();
}